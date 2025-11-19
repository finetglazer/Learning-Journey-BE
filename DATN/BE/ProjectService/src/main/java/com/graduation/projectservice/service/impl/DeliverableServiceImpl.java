package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Deliverable;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.model.PM_Task;
import com.graduation.projectservice.model.PM_TaskAssignee;
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

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

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

            PM_Deliverable savedDeliverable = deliverableRepository.save(deliverable);

            log.info(Constant.LOG_DELIVERABLE_CREATED, savedDeliverable.getDeliverableId(), key, projectId);

            Map<String, Object> data = new HashMap<>();
            data.put("deliverableId", savedDeliverable.getDeliverableId());
            data.put("key", savedDeliverable.getKey());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_CREATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to create deliverable for project {} by user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDeliverable(Long userId, Long projectId, Long deliverableId, UpdateDeliverableRequest request) {
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
                    null
            );

        } catch (Exception e) {
            log.error("Failed to update deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
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
                    null
            );

        } catch (Exception e) {
            log.error("Failed to delete deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    public BaseResponse<?> getProjectStructure(Long projectId, Long userId) {
        log.info(Constant.LOG_RETRIEVING_PROJECT_STRUCTURE, projectId, userId);

        // Check authorization - both owner and member can view
        authHelper.requireActiveMember(projectId, userId);

        // Get all deliverables for the project
        List<PM_Deliverable> deliverables = deliverableRepository.findByProjectIdOrderByOrderAsc(projectId);

        // Convert to DTOs
        List<DeliverableStructureDTO> deliverableDTOs = deliverables.stream()
                .map(this::convertToStructureDTO)
                .toList();

        log.info(Constant.LOG_PROJECT_STRUCTURE_RETRIEVED, projectId, deliverableDTOs.size());

        return new BaseResponse<>(1, Constant.PROJECT_STRUCTURE_RETRIEVED_SUCCESS, deliverableDTOs);
    }

    private DeliverableStructureDTO convertToStructureDTO(PM_Deliverable deliverable) {
        List<PhaseDTO> phaseDTOs = deliverable.getPhases().stream()
                .map(phase -> new PhaseDTO(
                        phase.getPhaseId(),
                        phase.getName(),
                        phase.getKey(),
                        phase.getOrder(),
                        phase.getTasks().stream()
                                .map(this::convertToTaskDTO).toList()
                ))
                .toList();

        return new DeliverableStructureDTO(
                deliverable.getDeliverableId(),
                deliverable.getName(),
                deliverable.getKey(),
                deliverable.getOrder(),
                phaseDTOs
        );
    }

    private TaskDTO convertToTaskDTO(PM_Task task) {
        // Get assignees with avatar URLs
        List<AssigneeDTO> assigneeDTOs = getAssigneeDTOs(task.getAssignees());

        return new TaskDTO(
                task.getTaskId(),
                task.getName(),
                task.getKey(),
                formatStatus(task.getStatus()),
                formatPriority(task.getPriority()),
                task.getOrder(),
                assigneeDTOs
        );
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
        return switch (status) {
            case TO_DO -> "To do";
            case IN_PROGRESS -> "In progress";
            case IN_REVIEW -> "In review";
            case DONE -> "Done";
        };
    }

    private String formatPriority(TaskPriority priority) {
        return switch (priority) {
            case MINOR -> "Minor";
            case MEDIUM -> "Medium";
            case MAJOR -> "Major";
            case CRITICAL -> "Critical";
        };
    }
}