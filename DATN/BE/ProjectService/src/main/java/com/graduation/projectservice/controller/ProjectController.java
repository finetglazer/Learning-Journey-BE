package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.ProjectListResponse;
import com.graduation.projectservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<BaseResponse<ProjectListResponse>> getUserProjects(
            @RequestHeader("X-User-Id") Long userId) {

        log.info(Constant.LOG_GET_PROJECTS_REQUEST, userId);

        BaseResponse<ProjectListResponse> response = projectService.getUserProjects(userId);

        return ResponseEntity.ok(response);
    }
}