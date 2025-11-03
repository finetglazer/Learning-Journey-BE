package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.*;
import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface MonthPlanService {

    /**
     * Create a new month plan for a user
     * Auto-copies routines from previous month
     * Creates 4-5 WeekPlan records
     *
     * @param userId The authenticated user ID
     * @param request The month plan creation request
     * @return BaseResponse containing the created month plan details
     */
    BaseResponse<?> createMonthPlan(Long userId, CreateMonthPlanRequest request);

    /**
     * Retrieve a complete month plan by ID
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @return BaseResponse containing the month plan details
     */
    BaseResponse<?> getMonthPlan(Long userId, Long monthPlanId);

    /**
     * Get a month plan ID by its year and month
     *
     * @param userId The authenticated user ID
     * @param year The year (e.g., 2025)
     * @param month The month (e.g., 11)
     * @return BaseResponse containing the month plan ID, or not found message.
     */
    BaseResponse<?> getMonthPlanIdByDate(Long userId, int year, int month);

    /**
     * Add a big task to a month plan
     * Calculates affected week plans based on date range overlap
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param request The big task creation request
     * @return BaseResponse containing the created big task details and affected week plans
     */
    BaseResponse<?> addBigTask(Long userId, Long monthPlanId, AddBigTaskRequest request);

    /**
     * Get a single big task by its ID, ensuring it belongs to the user and the month plan.
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID
     * @return BaseResponse containing the big task details
     */
    BaseResponse<?> getBigTaskById(Long userId, Long monthPlanId, Long bigTaskId);

    /**
     * Delete a single big task by its ID.
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID to delete
     * @return BaseResponse containing a success message
     */
    BaseResponse<?> deleteBigTask(Long userId, Long monthPlanId, Long bigTaskId);

    /**
     * Add an event to a month plan and auto-schedule it to calendar
     * Links event to the appropriate week plan
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param request The event creation request
     * @return BaseResponse containing the created event details and week plan ID
     */
    BaseResponse<?> addEvent(Long userId, Long monthPlanId, AddEventRequest request);

    /**
     * Add a new unscheduled task to a big task
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID (for validation)
     * @param bigTaskId The parent big task ID
     * @param request The request containing the new task's name and note
     * @return BaseResponse containing the created task details
     */
    BaseResponse<?> addUnscheduledTask(Long userId, Long monthPlanId, Long bigTaskId, AddUnscheduledTaskRequest request);

    /**
     * Update a single big task by its ID
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID
     * @param request The request containing the updated details
     * @return Response containing the updated big task details
     */
    BaseResponse<?> updateBigTask(Long userId, Long monthPlanId, Long bigTaskId, UpdateBigTaskRequest request);

    /**
     * Update an unscheduled task (name and note)
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param bigTaskId The parent big task ID
     * @param unscheduledTaskId The ID of the unscheduled task (which is a CalendarItem ID)
     * @param request The request containing the new id, name, and note
     * @return BaseResponse containing the updated unscheduled task details
     */
    BaseResponse<?> updateUnscheduledTask(Long userId, Long monthPlanId, Long bigTaskId, Long unscheduledTaskId, UpdateUnscheduledTaskRequest request);

    /**
     * Update the approved routine names list for a month plan
     * Returns the differences (added and removed routines)
     *
     * @param userId The authenticated user ID
     * @param monthPlanId The month plan ID
     * @param request The routine list update request
     * @return BaseResponse containing the changes made to the routine list
     */
    BaseResponse<?> updateRoutineList(Long userId, Long monthPlanId, UpdateRoutineListRequest request);
}