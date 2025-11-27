package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.CreateTaskRequest;
import com.graduation.projectservice.payload.request.GetTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskRequest;
import com.graduation.projectservice.payload.request.UpdateTaskStatusRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/api/pm/projects/{projectId}/tasks")
    public ResponseEntity<BaseResponse<?>> getTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody GetTaskRequest request) {

        log.info(Constant.LOG_GET_TASK_REQUEST, projectId, userId);

        BaseResponse<?> response = taskService.getTasks(userId, projectId, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/api/pm/projects/{projectId}/phases/{phaseId}/tasks")
    public ResponseEntity<BaseResponse<?>> createTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @Valid @RequestBody CreateTaskRequest request) {

        log.info(Constant.LOG_POST_TASK_REQUEST, phaseId, projectId, userId);

        BaseResponse<?> response = taskService.createTask(userId, projectId, phaseId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/pm/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<BaseResponse<?>> updateTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        log.info(Constant.LOG_PUT_TASK_REQUEST, taskId, projectId, userId);

        BaseResponse<?> response = taskService.updateTask(userId, projectId, taskId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/pm/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<BaseResponse<?>> deleteTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        log.info(Constant.LOG_DELETE_TASK_REQUEST, taskId, projectId, userId);

        BaseResponse<?> response = taskService.deleteTask(userId, projectId, taskId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/pm/projects/{projectId}/tasks/{taskId}/status")
    public ResponseEntity<BaseResponse<?>> updateTaskStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {

        log.info(Constant.LOG_PUT_TASK_STATUS_REQUEST, taskId, projectId, userId);

        BaseResponse<?> response = taskService.updateTaskStatus(userId, projectId, taskId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/pm/projects/user/tasks")
    public ResponseEntity<BaseResponse<?>> getUserProjectTasks(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Request received: Get active tasks for user {}", userId);

        BaseResponse<?> response = taskService.getUserProjectTasks(userId);

        return ResponseEntity.ok(response);
    }
}