package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.CreateMonthPlanRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.MonthPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/calendar/planning/months")
@RequiredArgsConstructor
public class MonthPlanController {

    private final MonthPlanService monthPlanService;

    /**
     * Create a new month plan
     * Auto-copies routines from previous month
     * Creates 4-5 WeekPlan records
     *
     * @param userId Extracted from X-User-Id header
     * @param request The month plan creation request
     * @return Response containing the created month plan details
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createMonthPlan(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateMonthPlanRequest request) {

        try {
            log.info("Creating month plan: userId={}, year={}, month={}",
                    userId, request.getYear(), request.getMonth());

            BaseResponse<?> response = monthPlanService.createMonthPlan(userId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create month plan: userId={}, year={}, month={}",
                    userId, request.getYear(), request.getMonth(), e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to create month plan", null)
            );
        }
    }

    /**
     * Get a complete month plan by ID
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @return Response containing the month plan details
     */
    @GetMapping("/{monthPlanId}")
    public ResponseEntity<BaseResponse<?>> getMonthPlan(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId) {

        try {
            log.info("Getting month plan: userId={}, monthPlanId={}", userId, monthPlanId);

            BaseResponse<?> response = monthPlanService.getMonthPlan(userId, monthPlanId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get month plan: userId={}, monthPlanId={}", userId, monthPlanId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve month plan", null)
            );
        }
    }
}