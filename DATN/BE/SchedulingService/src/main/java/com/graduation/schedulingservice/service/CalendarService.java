package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface CalendarService {

    /**
     * Create a default personal calendar for a new user
     * Called internally when user completes registration
     */
    BaseResponse<?> createDefaultCalendar(Long userId);
}