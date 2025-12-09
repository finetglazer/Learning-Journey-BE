package com.graduation.userservice.service;

import com.graduation.userservice.payload.request.UpdateTimezoneRequest;
import com.graduation.userservice.payload.response.BaseResponse;

public interface UserSettingsService {
    // Add these to your existing UserSettingsService.java
    BaseResponse<?> getTimezone(Long userId);

    BaseResponse<?> updateTimezone(Long userId, UpdateTimezoneRequest request);
}