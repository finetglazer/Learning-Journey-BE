package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.UpdateDailyLimitsRequest;
import com.graduation.schedulingservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.UserConstraintsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/calendar/constraints")
@RequiredArgsConstructor
public class UserConstraintsController {

    private final UserConstraintsService userConstraintsService;

    @GetMapping("/sleep-hours")
    public ResponseEntity<?> getSleepHours(@RequestHeader("X-User-Id") Long userId) {
        // NOTE: I am using a Header "X-User-Id". Ensure your Gateway passes this!
        // Or if you use a custom Principal object, extract ID from there.
        return ResponseEntity.ok(userConstraintsService.getSleepHours(userId));
    }

    @PutMapping("/sleep-hours")
    public ResponseEntity<?> updateSleepHours(
            @Valid @RequestBody UpdateSleepHoursRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userConstraintsService.updateSleepHours(userId, request));
    }

    @GetMapping("/daily-limits")
    public ResponseEntity<?> getDailyLimits(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userConstraintsService.getDailyLimits(userId));
    }

    @PutMapping("/daily-limits")
    public ResponseEntity<?> updateDailyLimits(
            @Valid @RequestBody UpdateDailyLimitsRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userConstraintsService.updateDailyLimits(userId, request));
    }
}