package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.TimezoneConversionRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.CalendarItemService;
import com.graduation.schedulingservice.service.CalendarService; // ADD THIS
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final CalendarItemService calendarItemService;
    private final CalendarService calendarService; // ADD THIS

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

    // ADD THIS NEW ENDPOINT
    /**
     * Create default calendar for a new user
     * Called by UserService after email verification
     */
    @PostMapping("/{userId}/default-calendar")
    public ResponseEntity<?> createDefaultCalendar(@PathVariable Long userId) {
        log.info("Creating default calendar for user {}", userId);

        BaseResponse<?> response = calendarService.createDefaultCalendar(userId);
        return ResponseEntity.ok(response);
    }
}