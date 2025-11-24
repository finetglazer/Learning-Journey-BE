package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.request.CreateRiskRequest;
import com.graduation.projectservice.payload.request.UpdateRiskRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pm/projects/{projectId}/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    public BaseResponse<?> getRisks(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String assignee
    ) {
        return riskService.getRisks(userId, projectId, page, limit, search, assignee);
    }

    @PostMapping
    public BaseResponse<?> createRisk(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestBody CreateRiskRequest request
    ) {
        return riskService.createRisk(userId, projectId, request);
    }

    @PutMapping("/{riskId}")
    public BaseResponse<?> updateRisk(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long riskId,
            @RequestBody UpdateRiskRequest request
    ) {
        return riskService.updateRisk(userId, projectId, riskId, request);
    }

    @DeleteMapping("/{riskId}")
    public BaseResponse<?> deleteRisk(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long riskId
    ) {
        return riskService.deleteRisk(userId, projectId, riskId);
    }
}