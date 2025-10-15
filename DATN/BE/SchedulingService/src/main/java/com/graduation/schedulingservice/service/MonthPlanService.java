package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.CreateMonthPlanRequest;
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
}