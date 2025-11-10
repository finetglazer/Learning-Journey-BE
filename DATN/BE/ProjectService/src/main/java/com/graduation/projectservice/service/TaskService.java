package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskStatusRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface TaskService {

    BaseResponse<?> createTask(Long userId, Long projectId, Long phaseId, CreateTaskRequest request);

    BaseResponse<?> updateTask(Long userId, Long projectId, Long taskId, UpdateTaskRequest request);

    BaseResponse<?> deleteTask(Long userId, Long projectId, Long taskId);

    BaseResponse<?> updateTaskStatus(Long userId, Long projectId, Long taskId, UpdateTaskStatusRequest request);
}