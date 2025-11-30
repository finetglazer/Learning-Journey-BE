package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.payload.request.UpdateTimelineDatesRequest;
import com.graduation.projectservice.payload.request.UpdateTimelineOffsetRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.TimelineItemDTO;
import com.graduation.projectservice.payload.response.TimelineMilestoneDTO;
import com.graduation.projectservice.payload.response.TimelineStructureResponse;
import com.graduation.projectservice.repository.*;
import com.graduation.projectservice.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final ProjectRepository projectRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectAuthorizationHelper projectAuthorizationHelper;
    private final MilestoneRepository milestoneRepository;
    @Override
    @Transactional
    public BaseResponse<?> updateTimelineDates(Long userId, Long projectId, UpdateTimelineDatesRequest request) {
        try {
            // 1. Auth: Strict Owner Only check
            projectAuthorizationHelper.requireOwner(projectId, userId);

            // 2. Basic Date Logic
            // Assign to local variables to avoid type-annotation mismatch warnings
            LocalDate reqStartDate = request.getStartDate();
            LocalDate reqEndDate = request.getEndDate();

            if (reqEndDate.isBefore(reqStartDate)) {
                return new BaseResponse<>(0, "End date cannot be before start date.", null);
            }

            // 3. Update Logic based on Item Type
            switch (request.getType()) {
                case TASK:
                    PM_Task task = taskRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Task not found"));

                    // Fetch Parent Phase
                    PM_Phase parentPhase = phaseRepository.findById(task.getPhaseId())
                            .orElseThrow(() -> new RuntimeException("Parent Phase not found"));

                    // Validation: Task must be inside Phase
                    String taskWarning = validateParentChildConstraint(
                            parentPhase.getStartDate(), parentPhase.getEndDate(),
                            reqStartDate, reqEndDate,
                            "Phase", "Task");

                    if (taskWarning != null) return new BaseResponse<>(0, taskWarning, null);

                    // Update
                    task.setStartDate(reqStartDate);
                    task.setEndDate(reqEndDate);
                    taskRepository.save(task);
                    break;

                case PHASE:
                    PM_Phase phase = phaseRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Phase not found"));

                    // Fetch Parent Deliverable
                    PM_Deliverable parentDel = deliverableRepository.findById(phase.getDeliverableId())
                            .orElseThrow(() -> new RuntimeException("Parent Deliverable not found"));

                    // Validation 1: Phase must be inside Deliverable
                    String phaseWarning = validateParentChildConstraint(
                            parentDel.getStartDate(), parentDel.getEndDate(),
                            reqStartDate, reqEndDate,
                            "Deliverable", "Phase");

                    if (phaseWarning != null) return new BaseResponse<>(0, phaseWarning, null);

                    // Validation 2: Phase must cover all its Tasks
                    String coverageWarning = validatePhaseCoverage(phase, reqStartDate, reqEndDate);
                    if (coverageWarning != null) return new BaseResponse<>(0, coverageWarning, null);

                    // Update
                    phase.setStartDate(reqStartDate);
                    phase.setEndDate(reqEndDate);
                    phaseRepository.save(phase);
                    break;

                case DELIVERABLE:
                    PM_Deliverable deliverable = deliverableRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Deliverable not found"));

                    // Validation: Deliverable must cover all its Phases
                    String delCoverageWarning = validateDeliverableCoverage(deliverable, reqStartDate, reqEndDate);
                    if (delCoverageWarning != null) return new BaseResponse<>(0, delCoverageWarning, null);

                    // Update
                    deliverable.setStartDate(reqStartDate);
                    deliverable.setEndDate(reqEndDate);
                    deliverableRepository.save(deliverable);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported type");
            }

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Item dates updated", Collections.emptyMap());

        } catch (Exception e) {
            log.error("Failed to update timeline dates: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getTimelineStructure(Long userId, Long projectId) {
        // 1. Auth Check: Owner or Member
        projectAuthorizationHelper.requireActiveMember(projectId, userId);

        // 2. Fetch Deliverables (Phases are fetched via relationship)
        // Note: Assuming Repository has findAllByProjectIdOrderByOrderAsc
        List<PM_Deliverable> deliverables = deliverableRepository.findAllByProjectIdOrderByOrderAsc(projectId);

        // 3. Map Deliverables and Phases to TimelineItemDTO
        List<TimelineItemDTO> items = deliverables.stream()
                .map(this::mapToTimelineItem)
                .collect(Collectors.toList());

        // 4. Fetch and Map Milestones
        List<PM_Milestone> pmMilestones = milestoneRepository.findAllByProjectIdOrderByDateAsc(projectId);
        List<TimelineMilestoneDTO> milestones = pmMilestones.stream()
                .map(m -> new TimelineMilestoneDTO(m.getMilestoneId(), m.getName(), m.getDate()))
                .collect(Collectors.toList());

        PM_Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // 5. Build Response Data
        TimelineStructureResponse data = TimelineStructureResponse.builder()
                .projectStartDate(project.getStartDate())
                .items(items)
                .milestones(milestones)
                .build();

        return new BaseResponse<>(1, "Timeline structure retrieved", data);
    }

    private TimelineItemDTO mapToTimelineItem(PM_Deliverable deliverable) {
        // Map Phases (Children)
        List<TimelineItemDTO> children = deliverable.getPhases().stream()
                .sorted(Comparator.comparing(phase -> phase.getOrder() != null ? phase.getOrder() : 0))
                .map(phase -> TimelineItemDTO.builder()
                        .id("phase-" + phase.getPhaseId()) // Prefix ID
                        .type("PHASE")
                        .name(phase.getName())
                        .startDate(phase.getStartDate())
                        .endDate(phase.getEndDate())
                        // Phases have no children in this view
                        .build())
                .collect(Collectors.toList());

        // Map Deliverable
        return TimelineItemDTO.builder()
                .id("del-" + deliverable.getDeliverableId()) // Prefix ID
                .type("DELIVERABLE")
                .name(deliverable.getName())
                .startDate(deliverable.getStartDate())
                .endDate(deliverable.getEndDate())
                .children(children)
                .build();
    }

    @Override
    @Transactional
    public BaseResponse<?> offsetTimelineItem(Long userId, Long projectId, UpdateTimelineOffsetRequest request) {
        try {
            // 1. Auth
            projectAuthorizationHelper.requireOwner(projectId, userId);

            int days = request.getOffsetDays();
            String type = request.getType().toUpperCase();

            // 2. Check Existence
            if (!isItemExist(request.getId(), type)) {
                if (type.equals("TASK")) {
                    return new BaseResponse<>(Constant.ERROR_STATUS, "Offset not supported for TASK type", null);
                }
                return new BaseResponse<>(Constant.ERROR_STATUS, type + " not found", null);
            }

            // 3. Logic based on Type
            switch (type) {
                case "PHASE":
                    PM_Phase phase = phaseRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Phase not found"));

                    // Fetch Parent Deliverable
                    PM_Deliverable parentDel = deliverableRepository.findById(phase.getDeliverableId())
                            .orElseThrow(() -> new RuntimeException("Parent Deliverable not found"));

                    // Calculate Proposed Dates
                    LocalDate newStart = phase.getStartDate() != null ? phase.getStartDate().plusDays(days) : null;
                    LocalDate newEnd = phase.getEndDate() != null ? phase.getEndDate().plusDays(days) : null;

                    // Validation: Proposed Phase dates must be inside Deliverable
                    // Note: If phase dates are null, we skip validation (or block, depending on logic. Here assuming strict check if they become non-null)
                    if (newStart != null && newEnd != null) {
                        String offsetWarning = validateParentChildConstraint(
                                parentDel.getStartDate(), parentDel.getEndDate(),
                                newStart, newEnd,
                                "Deliverable", "Phase");
                        if (offsetWarning != null) return new BaseResponse<>(0, offsetWarning, null);
                    } else {
                        // If Phase has no dates, we can't offset it effectively, but let's allow "shifting null" (which does nothing) or block?
                        // Assuming we block if parent dates are missing based on your rule.
                        if (parentDel.getStartDate() == null || parentDel.getEndDate() == null) {
                            return new BaseResponse<>(0, "Parent Deliverable dates are not set, cannot offset Phase.", null);
                        }
                    }

                    // Apply Shift
                    shiftPhase(phase, days);
                    phaseRepository.save(phase);
                    break;

                case "DELIVERABLE":
                    PM_Deliverable deliverable = deliverableRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Deliverable not found"));

                    // Deliverable is top-level (in this context), so it carries its children safely.
                    // We only shift if dates exist.
                    if (deliverable.getStartDate() != null)
                        deliverable.setStartDate(deliverable.getStartDate().plusDays(days));
                    if (deliverable.getEndDate() != null)
                        deliverable.setEndDate(deliverable.getEndDate().plusDays(days));

                    if (deliverable.getPhases() != null) {
                        for (PM_Phase childPhase : deliverable.getPhases()) {
                            shiftPhase(childPhase, days);
                        }
                    }
                    deliverableRepository.save(deliverable);
                    break;

                default:
                    return new BaseResponse<>(Constant.ERROR_STATUS, "Offset only supported for PHASE and DELIVERABLE", null);
            }

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Offset successful", Collections.emptyMap());

        } catch (Exception e) {
            log.error("Failed to offset timeline item: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void shiftPhase(PM_Phase phase, int days) {
        if (phase.getStartDate() != null) phase.setStartDate(phase.getStartDate().plusDays(days));
        if (phase.getEndDate() != null) phase.setEndDate(phase.getEndDate().plusDays(days));

        if (phase.getTasks() != null) {
            for (PM_Task task : phase.getTasks()) {
                if (task.getStartDate() != null) task.setStartDate(task.getStartDate().plusDays(days));
                if (task.getEndDate() != null) task.setEndDate(task.getEndDate().plusDays(days));
            }
        }
    }

    private boolean isItemExist(Long id, String type) {
        return switch (type) {
            case "TASK" -> taskRepository.existsById(id);
            case "PHASE" -> phaseRepository.existsById(id);
            case "DELIVERABLE" -> deliverableRepository.existsById(id);
            default -> false;
        };
    }

    /**
     * Validates that Child dates are strictly within Parent dates.
     * Also enforces the "Parent must have dates set" rule.
     */
    private String validateParentChildConstraint(LocalDate pStart, LocalDate pEnd, LocalDate cStart, LocalDate cEnd, String pName, String cName) {
        // Rule: If Parent dates are null, Child cannot set dates.
        if (pStart == null || pEnd == null) {
            return String.format("%s start/end dates are not set. Cannot set %s dates.", pName, cName);
        }

        // Rule: Child Start >= Parent Start
        if (cStart.isBefore(pStart)) {
            return String.format("%s start date (%s) cannot be before %s start date (%s).", cName, cStart, pName, pStart);
        }

        // Rule: Child End <= Parent End
        if (cEnd.isAfter(pEnd)) {
            return String.format("%s end date (%s) cannot be after %s end date (%s).", cName, cEnd, pName, pEnd);
        }

        return null; // Valid
    }

    /**
     * Validates that a Phase covers all its Tasks when Phase dates change.
     */
    private String validatePhaseCoverage(PM_Phase phase, LocalDate newPhaseStart, LocalDate newPhaseEnd) {
        if (phase.getTasks() == null || phase.getTasks().isEmpty()) return null;

        for (PM_Task task : phase.getTasks()) {
            if (task.getStartDate() != null && task.getStartDate().isBefore(newPhaseStart)) {
                return String.format("Phase start date cannot be after Task '%s' start (%s).", task.getName(), task.getStartDate());
            }
            if (task.getEndDate() != null && task.getEndDate().isAfter(newPhaseEnd)) {
                return String.format("Phase end date cannot be before Task '%s' end (%s).", task.getName(), task.getEndDate());
            }
        }
        return null;
    }

    /**
     * Validates that a Deliverable covers all its Phases when Deliverable dates change.
     */
    private String validateDeliverableCoverage(PM_Deliverable deliverable, LocalDate newDelStart, LocalDate newDelEnd) {
        if (deliverable.getPhases() == null || deliverable.getPhases().isEmpty()) return null;

        for (PM_Phase phase : deliverable.getPhases()) {
            if (phase.getStartDate() != null && phase.getStartDate().isBefore(newDelStart)) {
                return String.format("Deliverable start date cannot be after Phase '%s' start (%s).", phase.getName(), phase.getStartDate());
            }
            if (phase.getEndDate() != null && phase.getEndDate().isAfter(newDelEnd)) {
                return String.format("Deliverable end date cannot be before Phase '%s' end (%s).", phase.getName(), phase.getEndDate());
            }
        }
        return null;
    }
}