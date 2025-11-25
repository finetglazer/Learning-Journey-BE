package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.response.BaseResponse;

public interface ProjectSummaryService {

    BaseResponse<?> getDeliverableProgress(Long userId, Long projectId);

    BaseResponse<?> getTeammateWorkload(Long userId, Long projectId);

    BaseResponse<?> getTaskStats(Long userId, Long projectId);

    BaseResponse<?> getProjectTimeline(Long userId, Long projectId);

    BaseResponse<?> getActiveRisks(Long userId, Long projectId);
}