package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.UpdateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import java.util.List;


public interface CalendarItemService {

    /**
     * Create a new calendar item (Task, Routine, or Event)
     * @param userId The authenticated user ID
     * @param request The calendar item creation request
     * @return BaseResponse containing creation result
     */
    BaseResponse<?> createItem(Long userId, CreateCalendarItemRequest request);

    /**
     * Get a single calendar item by ID
     * @param userId The authenticated user ID
     * @param itemId The calendar item ID
     * @return BaseResponse containing item details
     */
    BaseResponse<?> getItemById(Long userId, Long itemId);

    /**
     * Update an existing calendar item
     * @param userId The authenticated user ID
     * @param itemId The calendar item ID
     * @param request The update request
     * @return BaseResponse containing update result
     */
    BaseResponse<?> updateItem(Long userId, Long itemId, UpdateCalendarItemRequest request);

    /**
     * Delete a calendar item
     * @param userId The authenticated user ID
     * @param itemId The calendar item ID
     * @return BaseResponse containing deletion result
     */
    BaseResponse<?> deleteItem(Long userId, Long itemId);

    /**
     * Convert timezone for all calendar items of a user
     * @param userId The user ID
     * @param oldTimezone Previous timezone
     * @param newTimezone New timezone
     * @return BaseResponse containing conversion result
     */
    BaseResponse<?> convertUserTimezone(Long userId, String oldTimezone, String newTimezone);

    /**
     * Get calendar items by date range and view type.
     * @param userId The authenticated user ID.
     * @param view The view type (DAY, WEEK, MONTH, YEAR).
     * @param date The reference date string.
     * @param calendarIds A list of calendar IDs to filter by.
     * @return BaseResponse containing a list of scheduled items.
     */
    BaseResponse<?> getItemsByDateRange(Long userId, String view, String date, List<Long> calendarIds);

    /**
     * Get all unscheduled items for a user.
     * @param userId The authenticated user ID.
     * @param weekPlanId Optional ID to filter by a specific week plan.
     * @return BaseResponse containing lists of unscheduled tasks and routines.
     */
    BaseResponse<?> getUnscheduledItems(Long userId, Long weekPlanId);
}