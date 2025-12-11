package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateTaskRequest;
import com.graduation.projectservice.payload.request.GetTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskStatusRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface TaskService {
    BaseResponse<?> getTasks(Long userId, Long projectId, GetTaskRequest request);

    BaseResponse<?> getTaskById(Long taskId);

    BaseResponse<?> createTask(Long userId, Long projectId, Long phaseId, CreateTaskRequest request);

    BaseResponse<?> updateTask(Long userId, Long projectId, Long taskId, UpdateTaskRequest request);

    BaseResponse<?> deleteTask(Long userId, Long projectId, Long taskId);

    BaseResponse<?> updateTaskStatus(Long userId, Long projectId, Long taskId, UpdateTaskStatusRequest request);

    /**
     * Get all tasks for a specific phase
     */
    BaseResponse<?> getTasksByPhase(Long projectId, Long phaseId, Long userId);

    BaseResponse<?> getUserProjectTasks(Long userId);

    /**
     * Attach a file node to a task
     */
    BaseResponse<?> attachFileToTask(Long userId, Long projectId, Long taskId, Long nodeId);

    /**
     * Detach a file node from a task
     */
    BaseResponse<?> detachFileFromTask(Long userId, Long projectId, Long taskId, Long nodeId);

    /**
     * Get attachment count for a task (dynamic count)
     */
    Integer getAttachmentCount(Long taskId);

    /**
     * Get all attachments for a specific task
     */
    BaseResponse<?> getTaskAttachments(Long userId, Long projectId, Long taskId);
}