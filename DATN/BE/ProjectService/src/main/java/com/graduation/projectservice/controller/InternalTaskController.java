package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/internal/tasks")
@RequiredArgsConstructor
public class InternalTaskController {
    private final TaskService taskService;

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        log.info("Received get project task request");

        BaseResponse<?> response = taskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }
}