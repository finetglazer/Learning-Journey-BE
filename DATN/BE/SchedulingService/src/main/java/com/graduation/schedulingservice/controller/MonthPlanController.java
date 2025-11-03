package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.*;
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
     * Get a month plan ID by its year and month
     *
     * @param userId Extracted from X-User-Id header
     * @param year The year (e.g., 2025)
     * @param month The month (e.g., 11)
     * @return Response containing the month plan ID
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getMonthPlanIdByDate(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        try {
            log.info("Finding month plan ID by date: userId={}, year={}, month={}",
                    userId, year, month);

            // You will need to create this method in your MonthPlanService interface and implementation
            BaseResponse<?> response = monthPlanService.getMonthPlanIdByDate(userId, year, month);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to find month plan ID by date: userId={}, year={}, month={}",
                    userId, year, month, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve month plan ID", null)
            );
        }
    }

    /**
     * Add a new unscheduled task to a big task
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The parent big task ID
     * @param request The request containing the new task's name and note
     * @return BaseResponse containing the created unscheduled task details
     */
    @PostMapping("/{monthPlanId}/big-tasks/{bigTaskId}/unscheduled-tasks")
    public ResponseEntity<BaseResponse<?>> addUnscheduledTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @PathVariable Long bigTaskId,
            @Valid @RequestBody AddUnscheduledTaskRequest request) {
        try {
            log.info("Adding unscheduled task to big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            BaseResponse<?> response = monthPlanService.addUnscheduledTask(
                    userId, monthPlanId, bigTaskId, request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to add unscheduled task: bigTaskId={}", bigTaskId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to add unscheduled task", null)
            );
        }
    }

    /**
     * Update an unscheduled task (name and note)
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The parent big task ID
     * @param unscheduledTaskId The ID of the unscheduled task (which is a CalendarItem ID)
     * @param request The request containing the new name and note
     * @return BaseResponse containing the updated unscheduled task details
     */
    @PutMapping("/{monthPlanId}/big-tasks/{bigTaskId}/unscheduled-tasks/{unscheduledTaskId}")
    public ResponseEntity<BaseResponse<?>> updateUnscheduledTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @PathVariable Long bigTaskId,
            @PathVariable Long unscheduledTaskId,
            @Valid @RequestBody UpdateUnscheduledTaskRequest request) {
        try {
            log.info("Updating unscheduled task: userId={}, monthPlanId={}, bigTaskId={}, unscheduledTaskId={}",
                    userId, monthPlanId, bigTaskId, unscheduledTaskId);

            // You will need to create this method in your MonthPlanService
            BaseResponse<?> response = monthPlanService.updateUnscheduledTask(
                    userId, monthPlanId, bigTaskId, unscheduledTaskId, request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update unscheduled task: unscheduledTaskId={}", unscheduledTaskId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to update unscheduled task", null)
            );
        }
    }

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
     * Get a single big task by its ID
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID
     * @return Response containing the big task details
     */
    @GetMapping("/{monthPlanId}/big-tasks/{bigTaskId}")
    public ResponseEntity<BaseResponse<?>> getBigTaskById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @PathVariable Long bigTaskId) {

        try {
            log.info("Getting big task by ID: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // You will need to create this method in your MonthPlanService
            BaseResponse<?> response = monthPlanService.getBigTaskById(userId, monthPlanId, bigTaskId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve big task", null)
            );
        }
    }

    /**
     * Update a single big task by its ID
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID
     * @param request The request containing the updated details
     * @return Response containing the updated big task details
     */
    @PutMapping("/{monthPlanId}/big-tasks/{bigTaskId}")
    public ResponseEntity<BaseResponse<?>> updateBigTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @PathVariable Long bigTaskId,
            @Valid @RequestBody UpdateBigTaskRequest request) {

        try {
            log.info("Updating big task by ID: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            BaseResponse<?> response = monthPlanService.updateBigTask(userId, monthPlanId, bigTaskId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to update big task", null)
            );
        }
    }

    /**
     * Delete a single big task by its ID
     *
     * @param userId Extracted from X-User-Id header
     * @param monthPlanId The month plan ID
     * @param bigTaskId The big task ID
     * @return Response containing a success message
     */
    @DeleteMapping("/{monthPlanId}/big-tasks/{bigTaskId}")
    public ResponseEntity<BaseResponse<?>> deleteBigTask(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long monthPlanId,
            @PathVariable Long bigTaskId) {

        try {
            log.info("Deleting big task by ID: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // You will need to create this method in your MonthPlanService
            BaseResponse<?> response = monthPlanService.deleteBigTask(userId, monthPlanId, bigTaskId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to delete big task", null)
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