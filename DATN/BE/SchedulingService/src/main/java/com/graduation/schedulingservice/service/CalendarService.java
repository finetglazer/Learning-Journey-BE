package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.payload.response.CalendarListResponse;

public interface CalendarService {

    /**
     * Create a default personal calendar for a new user
     * Called internally when user completes registration
     */
    BaseResponse<?> createDefaultCalendar(Long userId);

    /**
     * Get all calendars for a specific user
     * @param userId The ID of the user
     * @return A response containing the list of calendars
     */
    BaseResponse<CalendarListResponse> getUserCalendars(Long userId);
}