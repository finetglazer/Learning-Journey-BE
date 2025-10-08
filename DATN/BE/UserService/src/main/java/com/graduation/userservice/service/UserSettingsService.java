package com.graduation.userservice.service;

import com.graduation.userservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.userservice.payload.response.BaseResponse;

public interface UserSettingsService {
    BaseResponse<?> getSleepHours(Long userId);
    BaseResponse<?> updateSleepHours(Long userId, UpdateSleepHoursRequest request);
}