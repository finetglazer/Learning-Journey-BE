package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.*;
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

    /**
     * Attach a file node to a task
     * POST /api/pm/projects/{projectId}/tasks/{taskId}/attachments
     */
    @PostMapping("/api/pm/projects/{projectId}/tasks/{taskId}/attachments")
    public ResponseEntity<BaseResponse<?>> attachFileToTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody AttachFileRequest request) {

        log.info("Request received: Attach file {} to task {} in project {} by user {}",
                request.getNodeId(), taskId, projectId, userId);

        BaseResponse<?> response = taskService.attachFileToTask(
                userId, projectId, taskId, request.getNodeId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Detach a file node from a task
     * DELETE /api/pm/projects/{projectId}/tasks/{taskId}/attachments/{nodeId}
     */
    @DeleteMapping("/api/pm/projects/{projectId}/tasks/{taskId}/attachments/{nodeId}")
    public ResponseEntity<BaseResponse<?>> detachFileFromTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long nodeId) {

        log.info("Request received: Detach file {} from task {} in project {} by user {}",
                nodeId, taskId, projectId, userId);

        BaseResponse<?> response = taskService.detachFileFromTask(
                userId, projectId, taskId, nodeId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/pm/projects/{projectId}/tasks/{taskId}/attachments")
    public ResponseEntity<BaseResponse<?>> getTaskAttachments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        log.info("Request received: Get attachments for task {} in project {} by user {}",
                taskId, projectId, userId);

        BaseResponse<?> response = taskService.getTaskAttachments(userId, projectId, taskId);

        return ResponseEntity.ok(response);
    }
}