package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.CreateMilestoneRequest;
import com.graduation.projectservice.payload.request.UpdateMilestoneRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.MilestoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createMilestone(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMilestoneRequest request) {

        log.info(Constant.LOG_POST_MILESTONE_REQUEST, projectId, userId);

        BaseResponse<?> response = milestoneService.createMilestone(userId, projectId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{milestoneId}")
    public ResponseEntity<BaseResponse<?>> updateMilestone(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody UpdateMilestoneRequest request) {

        log.info(Constant.LOG_PUT_MILESTONE_REQUEST, milestoneId, projectId, userId);

        BaseResponse<?> response = milestoneService.updateMilestone(userId, projectId, milestoneId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{milestoneId}")
    public ResponseEntity<BaseResponse<?>> deleteMilestone(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {

        log.info(Constant.LOG_DELETE_MILESTONE_REQUEST, milestoneId, projectId, userId);

        BaseResponse<?> response = milestoneService.deleteMilestone(userId, projectId, milestoneId);

        return ResponseEntity.ok(response);
    }
}