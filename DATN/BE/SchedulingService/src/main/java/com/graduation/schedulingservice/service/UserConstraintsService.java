package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.UpdateDailyLimitsRequest;
import com.graduation.schedulingservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface UserConstraintsService {
    BaseResponse<?> getSleepHours(Long userId);
    BaseResponse<?> updateSleepHours(Long userId, UpdateSleepHoursRequest request);
    BaseResponse<?> getDailyLimits(Long userId);
    BaseResponse<?> updateDailyLimits(Long userId, UpdateDailyLimitsRequest request);
}