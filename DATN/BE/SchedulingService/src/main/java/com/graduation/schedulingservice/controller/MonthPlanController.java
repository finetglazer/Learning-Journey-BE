package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.AddBigTaskRequest;
import com.graduation.schedulingservice.payload.request.AddEventRequest;
import com.graduation.schedulingservice.payload.request.CreateMonthPlanRequest;
import com.graduation.schedulingservice.payload.request.UpdateRoutineListRequest;
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

    /**
     * Add a big task to a month plan
     * Calculates affected week plans based on date range overlap
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param request The big task creation request
     * @return Response containing the created big task details and affected week plans
     */
    @PostMapping("/{monthPlanId}/big-tasks")
    public ResponseEntity<BaseResponse<?>> addBigTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @Valid @RequestBody AddBigTaskRequest request) {

        try {
            log.info("Adding big task to month plan: userId={}, monthPlanId={}, taskName={}",
                    userId, monthPlanId, request.getName());

            BaseResponse<?> response = monthPlanService.addBigTask(userId, monthPlanId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to add big task: userId={}, monthPlanId={}, taskName={}",
                    userId, monthPlanId, request.getName(), e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to add big task", null)
            );
        }
    }

    /**
     * Add an event to a month plan and auto-schedule it to calendar
     * Links event to the appropriate week plan
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param request The event creation request
     * @return Response containing the created event details and week plan ID
     */
    @PostMapping("/{monthPlanId}/events")
    public ResponseEntity<BaseResponse<?>> addEvent(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @Valid @RequestBody AddEventRequest request) {

        try {
            log.info("Adding event to month plan: userId={}, monthPlanId={}, eventName={}",
                    userId, monthPlanId, request.getName());

            BaseResponse<?> response = monthPlanService.addEvent(userId, monthPlanId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to add event: userId={}, monthPlanId={}, eventName={}",
                    userId, monthPlanId, request.getName(), e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to add event", null)
            );
        }
    }

    /**
     * Update the approved routine names list for a month plan
     * Returns the differences (added and removed routines)
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param request The routine list update request
     * @return Response containing the changes made to the routine list
     */
    @PutMapping("/{monthPlanId}/routines")
    public ResponseEntity<BaseResponse<?>> updateRoutineList(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @Valid @RequestBody UpdateRoutineListRequest request) {

        try {
            log.info("Updating routine list for month plan: userId={}, monthPlanId={}", userId, monthPlanId);

            BaseResponse<?> response = monthPlanService.updateRoutineList(userId, monthPlanId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update routine list: userId={}, monthPlanId={}", userId, monthPlanId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to update routine list", null)
            );
        }
    }
}