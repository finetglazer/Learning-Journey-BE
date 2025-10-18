package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.AddBigTaskRequest;
import com.graduation.schedulingservice.payload.request.AddEventRequest;
import com.graduation.schedulingservice.payload.request.CreateMonthPlanRequest;
import com.graduation.schedulingservice.payload.request.UpdateRoutineListRequest;
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