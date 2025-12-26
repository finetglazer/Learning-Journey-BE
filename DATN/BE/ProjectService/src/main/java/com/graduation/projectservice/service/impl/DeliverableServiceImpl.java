package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.model.enums.TaskPriority;
import com.graduation.projectservice.model.enums.TaskStatus;
import com.graduation.projectservice.payload.request.CreateDeliverableRequest;
import com.graduation.projectservice.payload.request.UpdateDeliverableRequest;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.DeliverableRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.service.DeliverableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.TemporalUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverableServiceImpl implements DeliverableService {

    private final DeliverableRepository deliverableRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationHelper authHelper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public BaseResponse<?> createDeliverable(Long userId, Long projectId, CreateDeliverableRequest request) {
        try {
            log.info(Constant.LOG_CREATING_DELIVERABLE, projectId, userId);

            // Authorization: Only OWNER can create deliverables
            authHelper.requireOwner(projectId, userId);

            // Get project and increment deliverable counter
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

            Long newCounter = project.getDeliverableCounter() + 1;
            project.setDeliverableCounter(newCounter);
            projectRepository.save(project);

            // Generate key with zero-padded counter (DEL-01, DEL-02, etc.)
            String key = String.format("DEL-%02d", newCounter);

            // Get the next order value
            Integer maxOrder = deliverableRepository.findMaxOrderByProjectId(projectId);
            Integer nextOrder = maxOrder + 1;

            // Create deliverable
            PM_Deliverable deliverable = new PM_Deliverable();
            deliverable.setProjectId(projectId);
            deliverable.setName(request.getName());
            deliverable.setKey(key);
            deliverable.setOrder(nextOrder);
            deliverable.setStartDate(project.getStartDate());
            deliverable.setEndDate(project.getStartDate().plusWeeks(2));

            PM_Deliverable savedDeliverable = deliverableRepository.save(deliverable);

            log.info(Constant.LOG_DELIVERABLE_CREATED, savedDeliverable.getDeliverableId(), key, projectId);

            Map<String, Object> data = new HashMap<>();
            data.put("deliverableId", savedDeliverable.getDeliverableId());
            data.put("key", savedDeliverable.getKey());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_CREATED_SUCCESS,
                    data);

        } catch (Exception e) {
            log.error("Failed to create deliverable for project {} by user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDeliverable(Long userId, Long projectId, Long deliverableId,
            UpdateDeliverableRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_DELIVERABLE, deliverableId, projectId, userId);

            // Authorization: Only OWNER can update deliverables
            authHelper.requireOwner(projectId, userId);

            PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_IN_PROJECT);
            }

            deliverable.setName(request.getName());
            deliverableRepository.save(deliverable);

            log.info(Constant.LOG_DELIVERABLE_UPDATED, deliverableId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_UPDATED_SUCCESS,
                    null);

        } catch (Exception e) {
            log.error("Failed to update deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteDeliverable(Long userId, Long projectId, Long deliverableId) {
        try {
            log.info(Constant.LOG_DELETING_DELIVERABLE, deliverableId, projectId, userId);

            // Authorization: Only OWNER can delete deliverables
            authHelper.requireOwner(projectId, userId);

            PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_IN_PROJECT);
            }

            // Delete will cascade to phases and tasks (when those entities are created)
            deliverableRepository.delete(deliverable);

            log.info(Constant.LOG_DELIVERABLE_DELETED, deliverableId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_DELETED_SUCCESS,
                    null);

        } catch (Exception e) {
            log.error("Failed to delete deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
        }
    }

    @Override
    public BaseResponse<?> getProjectStructure(Long projectId, Long userId, String search) {
        log.info(Constant.LOG_RETRIEVING_PROJECT_STRUCTURE, projectId, userId);

        // 1. Authorization check
        authHelper.requireActiveMember(projectId, userId);

        // 2. Determine if a search is active and standardize the keyword
        final String searchKeyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        final boolean isSearching = searchKeyword != null;

        // 3. Fetch all deliverables for the project (We fetch everything to search
        // nested items)
        // NOTE: If performance is an issue with very large projects, the repository
        // query should be expanded to join and filter nested items in SQL/JPQL.
        List<PM_Deliverable> allDeliverables = deliverableRepository.findByProjectIdOrderByOrderAsc(projectId);

        List<DeliverableStructureDTO> resultDeliverableDTOs = new ArrayList<>();

        // 4. Process and filter the deliverables
        for (PM_Deliverable deliverable : allDeliverables) {
            // Convert the entire structure and perform the deep search
            DeliverableStructureDTO dto = convertToStructureDTO(deliverable, isSearching, searchKeyword);

            // 5. Apply the final filtering criteria:
            // a) Deliverable name contains keyword (handled by helper methods:
            // `isNameMatch`)
            // b) Or any nested phase/task contained the keyword (handled by the flag:
            // `hasChildContainKeyword`)
            if (!isSearching || dto.isHasChildContainKeyword() || isNameMatch(deliverable.getName(), searchKeyword)) {
                resultDeliverableDTOs.add(dto);
            }
        }

        log.info(Constant.LOG_PROJECT_STRUCTURE_RETRIEVED, projectId, resultDeliverableDTOs.size());

        return new BaseResponse<>(1, Constant.PROJECT_STRUCTURE_RETRIEVED_SUCCESS, resultDeliverableDTOs);
    }

    // Helper method for case-insensitive and accent-insensitive matching (using a
    // simple lowercase check here)
    // For true accent/case insensitivity matching the database function, you'd need
    // a more advanced utility.
    private boolean isNameMatch(String name, String searchKeyword) {
        if (searchKeyword == null || name == null) {
            return false;
        }
        // Simple case-insensitive match for the service layer logic
        return name.toLowerCase().contains(searchKeyword.toLowerCase());
    }

    private DeliverableStructureDTO convertToStructureDTO(PM_Deliverable deliverable, boolean isSearching,
            String searchKeyword) {
        List<PhaseDTO> phaseDTOs = deliverable.getPhases().stream()
                .map(phase -> convertToPhaseDTO(phase, isSearching, searchKeyword))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PhaseDTO::getOrder))
                .toList();

        DeliverableStructureDTO dto = new DeliverableStructureDTO(
                deliverable.getDeliverableId(),
                deliverable.getName(),
                deliverable.getKey(),
                deliverable.getOrder(),
                false,
                phaseDTOs);

        if (isSearching) {
            // Check 1: Does the deliverable name itself match? (This is also checked in the
            // main service method)
            boolean selfMatch = isNameMatch(deliverable.getName(), searchKeyword);

            // Check 2: Did any child phase match? (This handles both phase-level and
            // task-level matches due to propagation)
            boolean childMatch = phaseDTOs.stream()
                    .anyMatch(PhaseDTO::isHasChildContainKeyword);

            // Propagate the flag: true if the deliverable matches OR if any child matches
            dto.setHasChildContainKeyword(selfMatch || childMatch);
        }

        return dto;
    }

    private PhaseDTO convertToPhaseDTO(PM_Phase phase, boolean isSearching, String searchKeyword) {
        // Map tasks to DTOs first
        List<TaskDTO> taskDTOs = phase.getTasks().stream()
                // We use the simplified conversion without the search keyword
                .map(task -> convertToTaskDTO(task, isSearching, searchKeyword))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TaskDTO::getOrder))
                .toList();

        PhaseDTO dto = new PhaseDTO(
                phase.getPhaseId(),
                phase.getName(),
                phase.getKey(),
                phase.getOrder(),
                false, // Initial value
                taskDTOs);

        if (isSearching) {
            // Check 1: Does the phase name itself match?
            boolean selfMatch = isNameMatch(phase.getName(), searchKeyword);

            // Check 2: Did any child task match?
            // We go back to the original PM_Phase model to check the task name,
            // as the TaskDTO doesn't have the flag.
            boolean childMatch = phase.getTasks().stream()
                    .anyMatch(task -> isNameMatch(task.getName(), searchKeyword));

            // Propagate the flag: true if the phase matches OR if any child matches
            dto.setHasChildContainKeyword(selfMatch || childMatch);

            if (selfMatch || childMatch) {
                return dto;
            } else {
                return null;
            }
        }

        return dto;
    }

    private TaskDTO convertToTaskDTO(PM_Task task, boolean isSearching, String searchKeyword) {
        // Get assignees with avatar URLs
        List<AssigneeDTO> assigneeDTOs = getAssigneeDTOs(task.getAssignees());

        TaskDTO dto = new TaskDTO(
                task.getTaskId(),
                task.getPhaseId(),
                task.getName(),
                task.getKey(),
                formatStatus(task.getStatus()),
                formatPriority(task.getPriority()),
                task.getOrder(),
                assigneeDTOs);

        if (isSearching) {
            if (isNameMatch(task.getName(), searchKeyword)) {
                return dto;
            } else {
                return null;
            }
        }
        return dto;
    }

    private List<AssigneeDTO> getAssigneeDTOs(Set<PM_TaskAssignee> assignees) {
        if (assignees == null || assignees.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = assignees.stream()
                .map(PM_TaskAssignee::getUserId)
                .toList();

        // Fetch user data from UserService
        List<UserBatchDTO> userBatch = userServiceClient.findUsersByIds(userIds);

        if (userBatch == null || userBatch.isEmpty()) {
            return List.of();
        }

        return userBatch.stream()
                .map(user -> new AssigneeDTO(user.getUserId(), user.getAvatarUrl()))
                .toList();
    }

    private String formatStatus(TaskStatus status) {
        switch (status) {
            case TO_DO:
                return "To do";
            case IN_PROGRESS:
                return "In progress";
            case IN_REVIEW:
                return "In review";
            case DONE:
                return "Done";
            default:
                return status.name();
        }
    }

    private String formatPriority(TaskPriority priority) {
        switch (priority) {
            case MINOR:
                return "Minor";
            case MEDIUM:
                return "Medium";
            case MAJOR:
                return "Major";
            case CRITICAL:
                return "Critical";
            default:
                return priority.name();
        }
    }

    @Override
    public BaseResponse<?> getProjectSkeleton(Long projectId, Long userId) {
        log.info("Retrieving lightweight project skeleton for project {} by user {}", projectId, userId);

        // 1. Authorization check
        authHelper.requireActiveMember(projectId, userId);

        // 2. Fetch all deliverables for the project
        List<PM_Deliverable> allDeliverables = deliverableRepository.findByProjectIdOrderByOrderAsc(projectId);

        // 3. Convert to skeleton DTOs (no task details, only counts)
        List<SkeletonDeliverableDTO> skeletonDTOs = allDeliverables.stream()
                .map(this::convertToSkeletonDTO)
                .toList();

        log.info("Project skeleton retrieved for project {}: {} deliverables", projectId, skeletonDTOs.size());

        return new BaseResponse<>(1, "Project skeleton retrieved successfully", skeletonDTOs);
    }

    /**
     * Convert Deliverable entity to Skeleton DTO
     * Includes phases with task counts but NO task details
     */
    private SkeletonDeliverableDTO convertToSkeletonDTO(PM_Deliverable deliverable) {
        List<SkeletonPhaseDTO> skeletonPhases = deliverable.getPhases().stream()
                .map(this::convertToSkeletonPhaseDTO)
                .sorted(Comparator.comparing(SkeletonPhaseDTO::getOrder))
                .toList();

        return new SkeletonDeliverableDTO(
                deliverable.getDeliverableId(),
                deliverable.getName(),
                deliverable.getKey(),
                deliverable.getOrder(),
                skeletonPhases);
    }

    /**
     * Convert Phase entity to Skeleton DTO
     * Includes task count but NO task details
     */
    private SkeletonPhaseDTO convertToSkeletonPhaseDTO(PM_Phase phase) {
        // Count tasks instead of loading full details
        Integer taskCount = phase.getTasks() != null ? phase.getTasks().size() : 0;

        return new SkeletonPhaseDTO(
                phase.getPhaseId(),
                phase.getName(),
                phase.getKey(),
                phase.getOrder(),
                phase.getDeliverableId(),
                taskCount);
    }
}