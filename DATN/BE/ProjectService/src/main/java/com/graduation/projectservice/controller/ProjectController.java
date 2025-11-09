package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.CreateProjectRequest;
import com.graduation.projectservice.payload.request.UpdateProjectRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<BaseResponse<?>> getUserProjects(
            @RequestHeader("X-User-Id") Long userId) {

        log.info(Constant.LOG_GET_PROJECTS_REQUEST, userId);

        BaseResponse<?> response = projectService.getUserProjects(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createProject(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateProjectRequest request) {

        log.info(Constant.LOG_POST_PROJECT_REQUEST, userId);

        BaseResponse<?> response = projectService.createProject(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<BaseResponse<?>> updateProject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {

        log.info(Constant.LOG_PUT_PROJECT_REQUEST, projectId, userId);

        BaseResponse<?> response = projectService.updateProject(userId, projectId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<BaseResponse<?>> deleteProject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info(Constant.LOG_DELETE_PROJECT_REQUEST, projectId, userId);

        BaseResponse<?> response = projectService.deleteProject(userId, projectId);

        return ResponseEntity.ok(response);
    }
}