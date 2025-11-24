package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Deliverable;
import com.graduation.projectservice.model.PM_Milestone;
import com.graduation.projectservice.model.PM_Phase;
import com.graduation.projectservice.model.PM_Task;
import com.graduation.projectservice.payload.request.UpdateTimelineDatesRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.TimelineItemDTO;
import com.graduation.projectservice.payload.response.TimelineMilestoneDTO;
import com.graduation.projectservice.payload.response.TimelineStructureResponse;
import com.graduation.projectservice.repository.DeliverableRepository;
import com.graduation.projectservice.repository.MilestoneRepository;
import com.graduation.projectservice.repository.PhaseRepository;
import com.graduation.projectservice.repository.TaskRepository;
import com.graduation.projectservice.service.DeliverableService;
import com.graduation.projectservice.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // Import this
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectAuthorizationHelper projectAuthorizationHelper;
    private final MilestoneRepository milestoneRepository;

    @Override
    @Transactional
    public BaseResponse<?> updateTimelineDates(Long userId, Long projectId, UpdateTimelineDatesRequest request) {
        try {
            log.info("User {} updating timeline dates for {} {} in project {}", userId, request.getType(), request.getId(), projectId);

            // 1. Authorization: Strict Owner Only check
            projectAuthorizationHelper.requireOwner(projectId, userId);

            // 2. Basic Date Validation
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date cannot be before start date.");
            }

            // 3. Update Logic based on Item Type
            switch (request.getType()) {
                case TASK:
                    PM_Task task = taskRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Task not found with ID: " + request.getId()));

                    task.setStartDate(request.getStartDate());
                    task.setEndDate(request.getEndDate());
                    taskRepository.save(task);
                    break;

                case PHASE:
                    PM_Phase phase = phaseRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Phase not found with ID: " + request.getId()));

                    phase.setStartDate(request.getStartDate());
                    phase.setEndDate(request.getEndDate());
                    phaseRepository.save(phase);
                    break;

                case DELIVERABLE:
                    PM_Deliverable deliverable = deliverableRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Deliverable not found with ID: " + request.getId()));

                    deliverable.setStartDate(request.getStartDate());
                    deliverable.setEndDate(request.getEndDate());
                    deliverableRepository.save(deliverable);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported type: " + request.getType());
            }

            // 4. Success Response
            // CHANGED: Use Collections.emptyMap() instead of new Object() to avoid Serialization error
            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    "Item dates updated",
                    Collections.emptyMap()
            );

        } catch (Exception e) {
            log.error("Failed to update timeline dates for project {}: {}", projectId, e.getMessage());
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
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

        // 5. Build Response Data
        TimelineStructureResponse data = TimelineStructureResponse.builder()
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
}