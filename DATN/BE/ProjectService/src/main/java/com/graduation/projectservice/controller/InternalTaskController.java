package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.response.TaskDTO;
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
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
        log.info("Received get project task request for taskId: {}", taskId);

        TaskDTO task = taskService.getTaskByIdForInternal(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }
}