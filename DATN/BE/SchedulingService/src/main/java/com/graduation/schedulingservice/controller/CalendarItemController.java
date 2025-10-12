package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.CalendarItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("create")
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
}