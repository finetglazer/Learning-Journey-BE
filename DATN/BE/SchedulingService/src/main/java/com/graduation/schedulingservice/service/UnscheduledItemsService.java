package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface UnscheduledItemsService {
    BaseResponse<?> getUnscheduledItemsGroupedByMonth(Long userId);
}