package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.UpdateTimelineDatesRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface TimelineService {
    BaseResponse<?> updateTimelineDates(Long userId, Long projectId, UpdateTimelineDatesRequest request);

    // New method
    BaseResponse<?> getTimelineStructure(Long userId, Long projectId);
}