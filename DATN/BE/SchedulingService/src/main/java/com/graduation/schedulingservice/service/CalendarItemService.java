package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface CalendarItemService {

    /**
     * Create a new calendar item (Task, Routine, or Event)
     * @param userId The authenticated user ID
     * @param request The calendar item creation request
     * @return BaseResponse containing creation result
     */
    BaseResponse<?> createItem(Long userId, CreateCalendarItemRequest request);

    /**
     * Convert timezone for all calendar items of a user
     * @param userId The user ID
     * @param oldTimezone Previous timezone
     * @param newTimezone New timezone
     * @return BaseResponse containing conversion result
     */
    BaseResponse<?> convertUserTimezone(Long userId, String oldTimezone, String newTimezone);
}