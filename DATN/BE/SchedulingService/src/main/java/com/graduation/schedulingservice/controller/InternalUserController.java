package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.TimezoneConversionRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.CalendarItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for service-to-service communication
 * These endpoints should NOT be exposed to external clients
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final CalendarItemService calendarItemService;

    /**
     * Convert timezone for all calendar items of a user
     * Called by UserService when user changes timezone
     */
    @PutMapping("/{userId}/timezone")
    public ResponseEntity<?> convertUserTimezone(
            @PathVariable Long userId,
            @Valid @RequestBody TimezoneConversionRequest request) {

        log.info("Received timezone conversion request for user {}: {} -> {}",
                userId, request.getOldTimezone(), request.getNewTimezone());

        BaseResponse<?> response = calendarItemService.convertUserTimezone(
                userId,
                request.getOldTimezone(),
                request.getNewTimezone()
        );

        return ResponseEntity.ok(response);
    }
}