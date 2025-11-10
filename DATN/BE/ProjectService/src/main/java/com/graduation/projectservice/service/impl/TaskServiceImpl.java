package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.payload.request.CreateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskStatusRequest;
import com.graduation.projectservice.payload.response.AssigneeDTO;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.repository.*;
import com.graduation.projectservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectAuthorizationHelper authHelper;

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

            // Create task
            PM_Task task = new PM_Task();
            task.setPhaseId(phaseId);
            task.setName(request.getName());
            task.setKey(key);
            task.setOrder(nextOrder);
            task.setDateAdded(LocalDate.now());

            PM_Task savedTask = taskRepository.save(task);

            log.info(Constant.LOG_TASK_CREATED, savedTask.getTaskId(), key, phaseId);

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", savedTask.getTaskId());
            data.put("key", savedTask.getKey());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.TASK_CREATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to create task for phase {} in project {} by user {}",
                    phaseId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
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
                    PM_TaskAssignee assignee = new PM_TaskAssignee(taskId, assigneeUserId);
                    taskAssigneeRepository.save(assignee);

                    // Note: avatar_url should be fetched from User service
                    // For now, returning placeholder
                    assigneeDTOs.add(new AssigneeDTO(assigneeUserId, "/avatars/" + assigneeUserId + ".png"));
                }
            }

            log.info(Constant.LOG_TASK_UPDATED, taskId, projectId);

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("assignees", assigneeDTOs);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.TASK_UPDATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to update task {} for project {} by user {}", taskId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
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
                    null
            );

        } catch (Exception e) {
            log.error("Failed to delete task {} for project {} by user {}", taskId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateTaskStatus(Long userId, Long projectId, Long taskId, UpdateTaskStatusRequest request) {
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
                    null
            );

        } catch (Exception e) {
            log.error("Failed to update status for task {} in project {} by user {}",
                    taskId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    private void verifyTaskBelongsToProject(PM_Task task, Long projectId) {
        PM_Phase phase = phaseRepository.findById(task.getPhaseId())
                .orElseThrow(() -> new RuntimeException(Constant.ERROR_PHASE_NOT_FOUND));

        PM_Deliverable deliverable = deliverableRepository.findById(phase.getDeliverableId())
                .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

        if (!deliverable.getProjectId().equals(projectId)) {
            throw new RuntimeException(Constant.ERROR_TASK_NOT_IN_PROJECT);
        }
    }
}