package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateMilestoneRequest;
import com.graduation.projectservice.payload.request.UpdateMilestoneRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface MilestoneService {
    BaseResponse<?> createMilestone(Long userId, Long projectId, CreateMilestoneRequest request);
    BaseResponse<?> updateMilestone(Long userId, Long projectId, Long milestoneId, UpdateMilestoneRequest request);
    BaseResponse<?> deleteMilestone(Long userId, Long projectId, Long milestoneId);
}