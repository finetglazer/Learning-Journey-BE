package com.graduation.userservice.service;

import com.graduation.userservice.payload.request.UpdateDailyLimitsRequest;
import com.graduation.userservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.userservice.payload.request.UpdateTimezoneRequest;
import com.graduation.userservice.payload.response.BaseResponse;

public interface UserSettingsService {
    BaseResponse<?> getSleepHours(Long userId);
    BaseResponse<?> updateSleepHours(Long userId, UpdateSleepHoursRequest request);

    // New methods
    BaseResponse<?> getDailyLimits(Long userId);
    BaseResponse<?> updateDailyLimits(Long userId, UpdateDailyLimitsRequest request);

    // Add these to your existing UserSettingsService.java
    BaseResponse<?> getTimezone(Long userId);

    BaseResponse<?> updateTimezone(Long userId, UpdateTimezoneRequest request);
}