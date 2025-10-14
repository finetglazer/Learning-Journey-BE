package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.UpdateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.CalendarItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/calendar/items")
@RequiredArgsConstructor
public class CalendarItemController {

    private final CalendarItemService calendarItemService;

    /**
     * Create a new calendar item (Task, Routine, or Event)
     *
     * @param userId Extracted from X-User-Id header (set by API Gateway after authentication)
     * @param request The calendar item creation request
     * @return Response containing the created item ID
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<?>> createItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateCalendarItemRequest request) {

        try {
            log.info("Creating calendar item: userId={}, type={}, name={}",
                    userId, request.getType(), request.getName());

            BaseResponse<?> response = calendarItemService.createItem(userId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_ITEM_CREATION_FAILED, userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to create calendar item", null)
            );
        }
    }

    /**
     * Get a single calendar item by ID
     *
     * @param userId Extracted from X-User-Id header
     * @param itemId The calendar item ID
     * @return Response containing item details
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<BaseResponse<?>> getItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {

        try {
            log.info("Getting calendar item: userId={}, itemId={}", userId, itemId);

            BaseResponse<?> response = calendarItemService.getItemById(userId, itemId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get calendar item: userId={}, itemId={}", userId, itemId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve calendar item", null)
            );
        }
    }

    /**
     * Update an existing calendar item
     *
     * @param userId Extracted from X-User-Id header
     * @param itemId The calendar item ID
     * @param request The update request
     * @return Response containing update result
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<BaseResponse<?>> updateItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCalendarItemRequest request) {

        try {
            log.info("Updating calendar item: userId={}, itemId={}", userId, itemId);

            BaseResponse<?> response = calendarItemService.updateItem(userId, itemId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update calendar item: userId={}, itemId={}", userId, itemId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to update calendar item", null)
            );
        }
    }

    /**
     * Delete a calendar item
     *
     * @param userId Extracted from X-User-Id header
     * @param itemId The calendar item ID
     * @return Response containing deletion result
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<BaseResponse<?>> deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {

        try {
            log.info("Deleting calendar item: userId={}, itemId={}", userId, itemId);

            BaseResponse<?> response = calendarItemService.deleteItem(userId, itemId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete calendar item: userId={}, itemId={}", userId, itemId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to delete calendar item", null)
            );
        }
    }

    /**
     * Get calendar items by date range and view type.
     * @param userId      Extracted from X-User-Id header.
     * @param view        View type: DAY, WEEK, MONTH, or YEAR.
     * @param date        Reference date in YYYY-MM-DD format.
     * @param calendarIds Comma-separated list of calendar IDs.
     * @return Response containing the list of items.
     */
    @GetMapping("")
    public ResponseEntity<BaseResponse<?>> getItemsByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("view") String view,
            @RequestParam("date") String date,
            @RequestParam("calendarIds") String calendarIds) {
        try {
            log.info(Constant.LOG_GET_ITEMS_BY_DATE_RANGE, userId, view, date, calendarIds);

            List<Long> calendarIdList = Arrays.stream(calendarIds.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            BaseResponse<?> response = calendarItemService.getItemsByDateRange(userId, view, date, calendarIdList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_ITEMS_BY_DATE_RANGE_FAILED, userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, Constant.MSG_ITEMS_RETRIEVAL_FAILED, null)
            );
        }
    }

    /**
     * Get all unscheduled items (routines and tasks).
     * @param userId     Extracted from X-User-Id header.
     * @param weekPlanId Optional filter for a specific week plan.
     * @return Response containing unscheduled items.
     */
    @GetMapping("/unscheduled")
    public ResponseEntity<BaseResponse<?>> getUnscheduledItems(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Long weekPlanId) {
        try {
            log.info(Constant.LOG_GET_UNSCHEDULED_ITEMS, userId, weekPlanId);

            BaseResponse<?> response = calendarItemService.getUnscheduledItems(userId, weekPlanId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_UNSCHEDULED_ITEMS_FAILED, userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, Constant.MSG_UNSCHEDULED_ITEMS_RETRIEVAL_FAILED, null)
            );
        }
    }
}