package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.exception.ForbiddenException;
import com.graduation.projectservice.exception.NotFoundException;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.model.enums.TaskPriority;
import com.graduation.projectservice.model.enums.TaskStatus;
import com.graduation.projectservice.payload.request.CreateTaskRequest;
import com.graduation.projectservice.payload.request.GetTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskStatusRequest;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.*;
import com.graduation.projectservice.service.TaskService;
import com.graduation.projectservice.config.KafkaConfig;
import com.graduation.projectservice.event.TaskUpdateEvent;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

        private static final int REPLY_PREVIEW_MAX_LENGTH = 50;
        private final TaskRepository taskRepository;
        private final PhaseRepository phaseRepository;
        private final DeliverableRepository deliverableRepository;
        private final ProjectRepository projectRepository;
        private final TaskAssigneeRepository taskAssigneeRepository;
        private final ProjectAuthorizationHelper authHelper;
        private final UserServiceClient userServiceClient;
        private final TaskAttachmentRepository taskAttachmentRepository;
        private final FileNodeRepository fileNodeRepository;
        private final TaskCommentRepository taskCommentRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Override
        public BaseResponse<?> getTasks(Long userId, Long projectId, GetTaskRequest request) {
                log.info(Constant.LOG_GETTING_TASK, projectId, userId);

                // 1. Authorization: Only active members can view tasks
                authHelper.requireActiveMember(projectId, userId);

                // 2. Fetch all tasks relevant to the project (must be joined via
                // Phase/Deliverable)
                // NOTE: This assumes your TaskRepository has a custom query to get tasks by
                // project ID.
                List<PM_Task> allProjectTasks = taskRepository.findAllTasksByProjectId(projectId);

                // 3. Apply Filtering
                List<PM_Task> filteredTasks = allProjectTasks.stream()
                                .filter(task -> applyTaskFilters(task, userId, request))
                                .toList();

                // 4. Convert to DTOs
                List<TaskDTO> filteredTaskDTOs = filteredTasks.stream()
                                .map(this::convertToTaskDTO)
                                .toList();

                log.info(Constant.LOG_TASK_RETRIEVED_SUCCESS, projectId, filteredTaskDTOs.size());

                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                Constant.LOG_TASK_RETRIEVED_SUCCESS,
                                filteredTaskDTOs);
        }

        @Override
        public BaseResponse<?> getTaskDetail(Long userId, Long projectId, Long taskId) {
                log.info("Retrieving full details for task {} in project {} by user {}", taskId, projectId, userId);

                // 1. Authorization: Only active members can view task details
                authHelper.requireActiveMember(projectId, userId);

                // 2. Fetch the Task core entity
                PM_Task task = taskRepository.findById(taskId)
                                .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

                // 3. Security: Verify task belongs to the project
                verifyTaskBelongsToProject(task, projectId);

                // 5. Get Attachments
                // Re-using the logic from your getTaskAttachments method
                List<PM_TaskAttachment> attachmentLinks = taskAttachmentRepository.findByTaskId(taskId);
                List<Long> nodeIds = attachmentLinks.stream().map(PM_TaskAttachment::getNodeId).toList();
                List<PM_FileNode> fileNodes = fileNodeRepository.findAllById(nodeIds);
                Map<Long, PM_FileNode> fileNodeMap = fileNodes.stream()
                                .collect(Collectors.toMap(PM_FileNode::getNodeId, Function.identity()));

                List<TaskAttachmentDetailDTO> attachments = attachmentLinks.stream()
                                .map(link -> {
                                        PM_FileNode node = fileNodeMap.get(link.getNodeId());
                                        return new TaskAttachmentDetailDTO(
                                                        (node != null) ? node.getNodeId() : null,
                                                        (node != null) ? node.getName() : "Unknown File",
                                                        (node != null) ? String.valueOf(node.getType()) : "unknown",
                                                        (node != null) ? node.getExtension() : "",
                                                        link.getAttachedAt(),
                                                        (node != null) ? node.getSizeBytes() : 0L);
                                }).toList();

                // 6. Get Comments
                // Assuming TaskCommentRepository has findByTaskIdOrderByCreatedAtDesc
                List<PM_TaskComment> commentEntities = taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
                List<TaskCommentDTO> comments = convertToCommentDTOs(commentEntities);

                // 7. Build Final Response
                TaskDetailDTO detail = TaskDetailDTO.builder()
                                .taskInfo(task)
                                .attachments(attachments)
                                .comments(comments)
                                .build();

                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                "Task details retrieved successfully",
                                detail);
        }

        /**
         * Helper to convert comment entities to DTOs and fetch user info
         */
        private List<TaskCommentDTO> convertToCommentDTOs(List<PM_TaskComment> entities) {
                if (entities.isEmpty())
                        return new ArrayList<>();

                // 1. Fetch User details in bulk
                List<Long> userIds = entities.stream().map(PM_TaskComment::getUserId).distinct().toList();
                List<UserBatchDTO> userBatch = userServiceClient.findUsersByIds(userIds);
                Map<Long, UserBatchDTO> userMap = userBatch.stream()
                                .collect(Collectors.toMap(UserBatchDTO::getUserId, Function.identity()));

                // 2. Fetch all unique Parent Comments in bulk to avoid N+1 queries
                Set<Long> parentIds = entities.stream()
                                .map(PM_TaskComment::getParentCommentId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                Map<Long, PM_TaskComment> parentCommentMap = parentIds.isEmpty() ? Collections.emptyMap()
                                : taskCommentRepository.findAllById(parentIds).stream()
                                                .collect(Collectors.toMap(PM_TaskComment::getCommentId,
                                                                Function.identity()));

                // 3. Map entities to DTOs
                return entities.stream().map(entity -> {
                        UserBatchDTO user = userMap.get(entity.getUserId());

                        ReplyInfoDTO replyInfo = null;
                        if (entity.getParentCommentId() != null) {
                                PM_TaskComment parent = parentCommentMap.get(entity.getParentCommentId());
                                if (parent != null) {
                                        // Assuming ReplyInfoDTO contains basic info of the parent being replied to
                                        replyInfo = new ReplyInfoDTO(
                                                        parent.getUserId(),
                                                        truncateForPreview(parent.getContent()));
                                }
                        }

                        return TaskCommentDTO.builder()
                                        .commentId(entity.getCommentId())
                                        .userId(entity.getUserId())
                                        .userName(user != null ? user.getName() : "Unknown User")
                                        .userAvatar(user != null ? user.getAvatarUrl() : null)
                                        .content(entity.getContent())
                                        .isEdited(entity.getUpdatedAt().isAfter(entity.getCreatedAt().plusSeconds(1)))
                                        .createdAt(entity.getCreatedAt())
                                        .replyInfo(replyInfo) // Correctly setting the object
                                        .build();
                }).toList();
        }

        /**
         * Truncate content to create a preview for replies
         * Max 50 characters + "..." suffix
         */
        private String truncateForPreview(String content) {
                if (content == null) {
                        return null;
                }
                if (content.length() <= REPLY_PREVIEW_MAX_LENGTH) {
                        return content;
                }
                return content.substring(0, REPLY_PREVIEW_MAX_LENGTH) + "...";
        }

        @Override
        public BaseResponse<?> getTaskById(Long taskId) {
                log.info(Constant.LOG_GETTING_TASK_BY_ID);

                Optional<PM_Task> optionalTask = taskRepository.findPM_TaskByTaskId(taskId);
                if (optionalTask.isEmpty()) {
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        Constant.ERROR_TASK_NOT_FOUND,
                                        null);
                }

                PM_Task task = optionalTask.get();
                TaskDTO dto = new TaskDTO(
                                task.getTaskId(),
                                task.getPhaseId(),
                                task.getName(),
                                task.getKey(),
                                task.getStatus().name(),
                                task.getPriority().name(),
                                task.getOrder());
                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                Constant.LOG_GET_TASK_SUCCESS,
                                dto);
        }

        // 5. Modified Helper Method for Filtering Tasks
        private boolean applyTaskFilters(PM_Task task, Long currentUserId, GetTaskRequest request) {
                boolean matchesSearch = true;
                boolean matchesIsMyTask = true;

                // Apply Search Filter (Case-insensitive name matching)
                if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
                        String searchLower = request.getSearch().trim().toLowerCase();
                        matchesSearch = task.getName().toLowerCase().contains(searchLower);
                }

                // Apply showMyTask Filter
                if (request.isShowMyTask()) {
                        // Check if the task's assignee set contains the current user's ID
                        matchesIsMyTask = task.getAssignees().stream()
                                        .anyMatch(assignee -> assignee.getUserId().equals(currentUserId));
                }

                return matchesSearch && matchesIsMyTask;
        }

        @Override
        @Transactional
        public BaseResponse<?> createTask(Long userId, Long projectId, Long phaseId, CreateTaskRequest request) {
                try {
                        log.info(Constant.LOG_CREATING_TASK, phaseId, projectId, userId);

                        // Authorization: Only OWNER can create tasks
                        authHelper.requireOwner(projectId, userId);

                        // Verify phase exists and belongs to project
                        PM_Phase phase = phaseRepository.findById(phaseId)
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_PHASE_NOT_FOUND));

                        PM_Deliverable deliverable = deliverableRepository.findById(phase.getDeliverableId())
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

                        if (!deliverable.getProjectId().equals(projectId)) {
                                throw new RuntimeException(Constant.ERROR_PHASE_NOT_IN_PROJECT);
                        }

                        // Get project and increment task counter
                        PM_Project project = projectRepository.findById(projectId)
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

                        Long newCounter = project.getTaskCounter() + 1;
                        project.setTaskCounter(newCounter);
                        projectRepository.save(project);

                        // Generate key (TSK-01, TSK-02, etc.)
                        String key = String.format("TSK-%02d", newCounter);

                        // Get next order value
                        Integer maxOrder = taskRepository.findMaxOrderByPhaseId(phaseId);
                        Integer nextOrder = maxOrder + 1;

                        LocalDate startDate = phase.getStartDate();
                        LocalDate endDate = phase.getEndDate().isBefore(startDate.plusWeeks(2)) ? phase.getEndDate()
                                        : startDate.plusWeeks(2);

                        // Create task
                        PM_Task task = new PM_Task();
                        task.setPhaseId(phaseId);
                        task.setName(request.getName());
                        task.setKey(key);
                        task.setOrder(nextOrder);
                        task.setDateAdded(LocalDate.now());
                        task.setStartDate(startDate);
                        task.setEndDate(endDate);

                        PM_Task savedTask = taskRepository.save(task);

                        log.info(Constant.LOG_TASK_CREATED, savedTask.getTaskId(), key, phaseId);

                        Map<String, Object> data = new HashMap<>();
                        data.put("taskId", savedTask.getTaskId());
                        data.put("key", savedTask.getKey());

                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        Constant.TASK_CREATED_SUCCESS,
                                        data);

                } catch (Exception e) {
                        log.error("Failed to create task for phase {} in project {} by user {}",
                                        phaseId, projectId, userId, e);
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        e.getMessage(),
                                        null);
                }
        }

        @Override
        @Transactional
        public BaseResponse<?> updateTask(Long userId, Long projectId, Long taskId, UpdateTaskRequest request) {
                try {
                        log.info(Constant.LOG_UPDATING_TASK, taskId, projectId, userId);

                        // Authorization: Only OWNER can update full task details
                        authHelper.requireOwner(projectId, userId);

                        PM_Task task = taskRepository.findById(taskId)
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_TASK_NOT_FOUND));

                        // Verify task belongs to project
                        verifyTaskBelongsToProject(task, projectId);

                        // Update task fields
                        task.setName(request.getName());
                        if (request.getStatus() != null) {
                                task.setStatus(request.getStatus());
                        }
                        if (request.getPriority() != null) {
                                task.setPriority(request.getPriority());
                        }
                        if (request.getStartDate() != null) {
                                task.setStartDate(request.getStartDate());
                        }
                        if (request.getEndDate() != null) {
                                task.setEndDate(request.getEndDate());
                        }

                        taskRepository.save(task);

                        // Update assignees
                        List<AssigneeDTO> assigneeDTOs = new ArrayList<>();
                        if (request.getAssigneeIds() != null) {
                                // Remove existing assignees
                                taskAssigneeRepository.deleteByTaskId(taskId);

                                // Add new assignees
                                for (Long assigneeUserId : request.getAssigneeIds()) {
                                        // check if active member
                                        try {
                                                authHelper.requireActiveMember(projectId, assigneeUserId);
                                        } catch (Exception e) {
                                                return new BaseResponse<>(0, "Not an active member: " + assigneeUserId,
                                                                null);
                                        }
                                        PM_TaskAssignee assignee = new PM_TaskAssignee(taskId, assigneeUserId);
                                        taskAssigneeRepository.save(assignee);

                                        // Note: avatar_url should be fetched from User service
                                        // For now, returning placeholder
                                        assigneeDTOs.add(new AssigneeDTO(assigneeUserId,
                                                        "/avatars/" + assigneeUserId + ".png"));
                                }
                        }

                        log.info(Constant.LOG_TASK_UPDATED, taskId, projectId);

                        try {
                                Set<Long> assigneeIdsForEvent;
                                if (request.getAssigneeIds() != null) {
                                        // Case 1: Assignees updated -> Notify new assignees
                                        assigneeIdsForEvent = new HashSet<>(request.getAssigneeIds());
                                } else {
                                        // Case 2: Assignees not changed -> Notify current assignees from DB entity
                                        assigneeIdsForEvent = task.getAssignees().stream()
                                                        .map(PM_TaskAssignee::getUserId)
                                                        .collect(Collectors.toSet());
                                }

                                TaskUpdateEvent event = new TaskUpdateEvent(
                                                taskId,
                                                projectId,
                                                userId,
                                                assigneeIdsForEvent,
                                                TaskUpdateEvent.ACTION_UPDATE);

                                kafkaTemplate.send(KafkaConfig.TOPIC_PROJECT_TASK_UPDATE, event);
                                log.info("Published TaskUpdateEvent for task {}", taskId);
                        } catch (Exception ex) {
                                log.error("Failed to publish TaskUpdateEvent for task {}", taskId, ex);
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("taskId", taskId);
                        data.put("assignees", assigneeDTOs);

                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        Constant.TASK_UPDATED_SUCCESS,
                                        data);

                } catch (Exception e) {
                        log.error("Failed to update task {} for project {} by user {}", taskId, projectId, userId, e);
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        e.getMessage(),
                                        null);
                }
        }

        @Override
        @Transactional
        public BaseResponse<?> deleteTask(Long userId, Long projectId, Long taskId) {
                try {
                        log.info(Constant.LOG_DELETING_TASK, taskId, projectId, userId);

                        // Authorization: Only OWNER can delete tasks
                        authHelper.requireOwner(projectId, userId);

                        PM_Task task = taskRepository.findById(taskId)
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_TASK_NOT_FOUND));

                        // Verify task belongs to project
                        verifyTaskBelongsToProject(task, projectId);

                        // Delete task (will cascade to assignees)
                        taskRepository.delete(task);

                        log.info(Constant.LOG_TASK_DELETED, taskId, projectId);

                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        Constant.TASK_DELETED_SUCCESS,
                                        null);

                } catch (Exception e) {
                        log.error("Failed to delete task {} for project {} by user {}", taskId, projectId, userId, e);
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        e.getMessage(),
                                        null);
                }
        }

        @Override
        @Transactional
        public BaseResponse<?> updateTaskStatus(Long userId, Long projectId, Long taskId,
                        UpdateTaskStatusRequest request) {
                try {
                        log.info(Constant.LOG_UPDATING_TASK_STATUS, taskId, projectId, userId);

                        // Authorization: OWNER or MEMBER can update status
                        authHelper.requireActiveMember(projectId, userId);

                        PM_Task task = taskRepository.findById(taskId)
                                        .orElseThrow(() -> new RuntimeException(Constant.ERROR_TASK_NOT_FOUND));

                        // Verify task belongs to project
                        verifyTaskBelongsToProject(task, projectId);

                        // Update only status
                        task.setStatus(request.getStatus());
                        taskRepository.save(task);

                        log.info(Constant.LOG_TASK_STATUS_UPDATED, taskId, request.getStatus(), projectId);

                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        Constant.TASK_STATUS_UPDATED_SUCCESS,
                                        null);

                } catch (Exception e) {
                        log.error("Failed to update status for task {} in project {} by user {}",
                                        taskId, projectId, userId, e);
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        e.getMessage(),
                                        null);
                }
        }

        private void verifyTaskBelongsToProject(PM_Task task, Long projectId) {
                PM_Phase phase = phaseRepository.findById(task.getPhaseId())
                                .orElseThrow(() -> new NotFoundException(Constant.ERROR_PHASE_NOT_FOUND));

                PM_Deliverable deliverable = deliverableRepository.findById(phase.getDeliverableId())
                                .orElseThrow(() -> new NotFoundException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

                if (!deliverable.getProjectId().equals(projectId)) {
                        throw new ForbiddenException(Constant.ERROR_TASK_NOT_IN_PROJECT);
                }
        }

        @Override
        public BaseResponse<?> getTasksByPhase(Long projectId, Long phaseId, Long userId) {
                log.info(Constant.LOG_RETRIEVING_PHASE_TASKS, phaseId, projectId, userId);

                // Check authorization - both owner and member can view
                authHelper.requireActiveMember(projectId, userId);

                // Verify phase belongs to project
                Long deliverableId = phaseRepository.findDeliverableIdByPhaseId(phaseId);
                if (deliverableId == null) {
                        throw new NotFoundException(Constant.ERROR_PHASE_NOT_FOUND);
                }

                PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                                .orElseThrow(() -> new NotFoundException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

                if (!deliverable.getProjectId().equals(projectId)) {
                        throw new NotFoundException(Constant.ERROR_PHASE_NOT_IN_PROJECT);
                }

                // Get all tasks for the phase
                List<PM_Task> tasks = taskRepository.findByPhaseIdOrderByOrderAsc(phaseId);

                // Convert to DTOs with assignees
                List<TaskDTO> taskDTOs = tasks.stream()
                                .map(this::convertToTaskDTO)
                                .toList();

                log.info(Constant.LOG_PHASE_TASKS_RETRIEVED, phaseId, taskDTOs.size());

                return new BaseResponse<>(1,
                                String.format(Constant.PHASE_TASKS_RETRIEVED_SUCCESS, phaseId),
                                taskDTOs);
        }

        private TaskDTO convertToTaskDTO(PM_Task task) {
                // Get assignees with avatar URLs
                List<AssigneeDTO> assigneeDTOs = getAssigneeDTOs(task.getAssignees());

                return new TaskDTO(
                                task.getTaskId(),
                                task.getPhaseId(),
                                task.getName(),
                                task.getKey(),
                                formatStatus(task.getStatus()),
                                formatPriority(task.getPriority()),
                                task.getOrder(),
                                assigneeDTOs);
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

                if (userBatch == null || userBatch.size() == 0) {
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

        @Override
        public BaseResponse<?> getUserProjectTasks(Long userId) {
                try {

                        // 1. Query the database using the projection
                        List<TaskProjectProjection> rawTasks = taskRepository.findActiveTasksForUser(userId);

                        // 2. Group by Project ID
                        Map<Long, List<TaskProjectProjection>> tasksByProject = rawTasks.stream()
                                        .collect(Collectors.groupingBy(TaskProjectProjection::getProjectId));

                        // 3. Map to DTOs
                        List<UserProjectTasksResponse.ProjectGroupDTO> projectGroups = new ArrayList<>();

                        for (Map.Entry<Long, List<TaskProjectProjection>> entry : tasksByProject.entrySet()) {
                                Long projectId = entry.getKey();
                                List<TaskProjectProjection> projectTasks = entry.getValue();

                                // Get project name from the first task in the list (all have same project name)
                                String projectName = projectTasks.get(0).getProjectName();

                                // Map tasks to Inner DTO
                                List<UserProjectTasksResponse.UserTaskItemDTO> taskItems = projectTasks.stream()
                                                .map(proj -> {
                                                        boolean isOverdue = proj.getEndDate() != null
                                                                        && proj.getEndDate().isBefore(LocalDate.now());

                                                        return new UserProjectTasksResponse.UserTaskItemDTO(
                                                                        proj.getTaskId(),
                                                                        proj.getTaskName(),
                                                                        proj.getEndDate(),
                                                                        isOverdue);
                                                })
                                                .toList();

                                projectGroups.add(new UserProjectTasksResponse.ProjectGroupDTO(
                                                projectId,
                                                projectName,
                                                taskItems));
                        }

                        // 4. Construct Final Data
                        UserProjectTasksResponse responseData = new UserProjectTasksResponse(projectGroups);

                        log.info("Retrieved {} active projects for user {}", projectGroups.size(), userId);

                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        "User project tasks retrieved successfully",
                                        responseData);

                } catch (Exception e) {
                        log.error("Error retrieving user project tasks", e);
                        // Return 200 OK with error status as requested
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        "Failed to retrieve tasks: " + e.getMessage(),
                                        null);
                }
        }

        @Override
        @Transactional
        public BaseResponse<?> attachFileToTask(Long userId, Long projectId, Long taskId, Long nodeId) {
                log.info("Attaching file node {} to task {} in project {} by user {}", nodeId, taskId, projectId,
                                userId);

                // 1. Authorization: Owner or Member can attach
                authHelper.requireActiveMember(projectId, userId);

                // 2. Verify task exists
                PM_Task task = taskRepository.findById(taskId)
                                .orElseThrow(() -> new NotFoundException("Task not found"));

                // 3. Verify task belongs to project (security: don't trust frontend)
                verifyTaskBelongsToProject(task, projectId);

                // 4. Verify file node exists
                PM_FileNode fileNode = fileNodeRepository.findById(nodeId)
                                .orElseThrow(() -> new NotFoundException("File not found"));

                // 5. Verify file node belongs to same project (security: don't trust frontend)
                if (!fileNode.getProjectId().equals(projectId)) {
                        throw new ForbiddenException("File does not belong to this project");
                }

                // 6. Check for duplicate attachment
                PM_TaskAttachmentId attachmentId = new PM_TaskAttachmentId(taskId, nodeId);
                if (taskAttachmentRepository.existsById(attachmentId)) {
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        "File is already attached to this task",
                                        null);
                }

                // 7. Create attachment
                PM_TaskAttachment attachment = new PM_TaskAttachment(taskId, nodeId, userId);
                PM_TaskAttachment saved = taskAttachmentRepository.save(attachment);

                log.info("File node {} attached to task {} successfully", nodeId, taskId);

                // 8. Build response
                TaskAttachmentDTO responseData = TaskAttachmentDTO.forAttach(
                                saved.getTaskId(),
                                saved.getNodeId(),
                                saved.getAttachedAt());

                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                "File attached to task",
                                responseData);
        }

        @Override
        @Transactional
        public BaseResponse<?> detachFileFromTask(Long userId, Long projectId, Long taskId, Long nodeId) {
                log.info("Detaching file node {} from task {} in project {} by user {}", nodeId, taskId, projectId,
                                userId);

                // 1. Authorization: Owner or Member can detach
                authHelper.requireActiveMember(projectId, userId);

                // 2. Verify task exists
                PM_Task task = taskRepository.findById(taskId)
                                .orElseThrow(() -> new NotFoundException("Task not found"));

                // 3. Verify task belongs to project (security: don't trust frontend)
                verifyTaskBelongsToProject(task, projectId);

                // 4. Check attachment exists
                PM_TaskAttachmentId attachmentId = new PM_TaskAttachmentId(taskId, nodeId);
                if (!taskAttachmentRepository.existsById(attachmentId)) {
                        return new BaseResponse<>(
                                        Constant.ERROR_STATUS,
                                        "Attachment not found",
                                        null);
                }

                // 5. Delete attachment (does NOT delete the actual file)
                taskAttachmentRepository.deleteById(attachmentId);

                log.info("File node {} detached from task {} successfully", nodeId, taskId);

                // 6. Build response
                TaskAttachmentDTO responseData = TaskAttachmentDTO.forDetach(
                                taskId,
                                nodeId,
                                LocalDateTime.now());

                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                "File detached from task",
                                responseData);
        }

        @Override
        public Integer getAttachmentCount(Long taskId) {
                return taskAttachmentRepository.countByTaskId(taskId);
        }

        @Override
        public BaseResponse<?> getTaskAttachments(Long userId, Long projectId, Long taskId) {
                log.info("Retrieving attachments for task {} in project {} by user {}", taskId, projectId, userId);

                // 1. Authorization: Owner or Member can view
                authHelper.requireActiveMember(projectId, userId);

                // 2. Verify task exists
                PM_Task task = taskRepository.findById(taskId)
                                .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

                // 3. Verify task belongs to project
                verifyTaskBelongsToProject(task, projectId);

                // 4. Fetch attachment links
                List<PM_TaskAttachment> attachments = taskAttachmentRepository.findByTaskId(taskId);

                if (attachments.isEmpty()) {
                        return new BaseResponse<>(
                                        Constant.SUCCESS_STATUS,
                                        "No attachments found",
                                        new ArrayList<>());
                }

                // 5. Fetch File Node Details
                // Extract nodeIds from attachments
                List<Long> nodeIds = attachments.stream()
                                .map(PM_TaskAttachment::getNodeId)
                                .toList();

                // Query FileNodeRepository for details (e.g., name, type)
                List<PM_FileNode> fileNodes = fileNodeRepository.findAllById(nodeIds);

                // Create a Map for faster lookup: nodeId -> PM_FileNode
                Map<Long, PM_FileNode> fileNodeMap = fileNodes.stream()
                                .collect(Collectors.toMap(PM_FileNode::getNodeId, Function.identity()));

                // 6. Combine data into DTOs
                List<TaskAttachmentDetailDTO> resultDTOs = attachments.stream()
                                .map(attachment -> {
                                        PM_FileNode node = fileNodeMap.get(attachment.getNodeId());

                                        // Handle case where file node might have been deleted but attachment link
                                        // remains (edge case)
                                        String fileName = (node != null) ? node.getName() : "Unknown File";
                                        String fileType = (node != null) ? String.valueOf(node.getType()) : "unknown"; // Assuming
                                                                                                                       // 'getType'
                                                                                                                       // or
                                                                                                                       // 'getExtension'
                                                                                                                       // exists
                                                                                                                       // on
                                                                                                                       // PM_FileNode

                                        return new TaskAttachmentDetailDTO(
                                                        (node != null) ? node.getNodeId() : null,
                                                        fileName,
                                                        fileType,
                                                        (node != null) ? node.getExtension() : "",
                                                        attachment.getAttachedAt(),
                                                        node.getSizeBytes()
                                        // attachment.getAddedByUserId() // The user who attached it
                                        );
                                })
                                .toList();

                log.info("Retrieved {} attachments for task {}", resultDTOs.size(), taskId);

                return new BaseResponse<>(
                                Constant.SUCCESS_STATUS,
                                "Task attachments retrieved successfully",
                                resultDTOs);
        }
}
