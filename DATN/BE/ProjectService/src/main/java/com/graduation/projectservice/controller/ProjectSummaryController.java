package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.ProjectSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects")
@RequiredArgsConstructor
public class ProjectSummaryController {

    private final ProjectSummaryService projectSummaryService;

    @GetMapping("/{projectId}/summary/deliverable-progress")
    public ResponseEntity<BaseResponse<?>> getDeliverableProgress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get deliverable progress for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getDeliverableProgress(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/summary/teammate-workload")
    public ResponseEntity<BaseResponse<?>> getTeammateWorkload(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get teammate workload for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getTeammateWorkload(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/summary/task-stats")
    public ResponseEntity<BaseResponse<?>> getTaskStats(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get task stats for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getTaskStats(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/summary/timeline")
    public ResponseEntity<BaseResponse<?>> getProjectTimeline(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get timeline for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getProjectTimeline(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/summary/active-risks")
    public ResponseEntity<BaseResponse<?>> getActiveRisks(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get active risks for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getActiveRisks(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/summary/dashboard")
    public ResponseEntity<BaseResponse<?>> getProjectDashboardSummary(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("Request: Get dashboard summary for project {} by user {}", projectId, userId);
        BaseResponse<?> response = projectSummaryService.getProjectDashboardSummary(userId, projectId);
        return ResponseEntity.ok(response);
    }
}