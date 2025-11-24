package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.UpdateTimelineDatesRequest;
import com.graduation.projectservice.payload.request.UpdateTimelineOffsetRequest; // Import new DTO
import com.graduation.projectservice.payload.response.BaseResponse;

public interface TimelineService {
    BaseResponse<?> updateTimelineDates(Long userId, Long projectId, UpdateTimelineDatesRequest request);
    BaseResponse<?> getTimelineStructure(Long userId, Long projectId);

    // New method for Offset Logic
    BaseResponse<?> offsetTimelineItem(Long userId, Long projectId, UpdateTimelineOffsetRequest request);
}