package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateRiskRequest;
import com.graduation.projectservice.payload.request.UpdateRiskRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface RiskService {
    BaseResponse<?> getRisks(Long userId, Long projectId, int page, int limit, String search, String assigneeFilter);
    BaseResponse<?> createRisk(Long userId, Long projectId, CreateRiskRequest request);
    BaseResponse<?> updateRisk(Long userId, Long projectId, Long riskId, UpdateRiskRequest request);
    BaseResponse<?> deleteRisk(Long userId, Long projectId, Long riskId);
}