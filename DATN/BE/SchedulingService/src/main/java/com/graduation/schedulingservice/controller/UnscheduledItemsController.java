package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.UnscheduledItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/calendar/planning")
@RequiredArgsConstructor
public class UnscheduledItemsController {

    private final UnscheduledItemsService unscheduledItemsService;

    /**
     * Get all unscheduled items across the next 6 months, grouped by month
     *
     * @param userId Extracted from X-User-Id header
     * @return Response containing unscheduled items grouped by month
     */
    @GetMapping("/unscheduled-items")
    public ResponseEntity<BaseResponse<?>> getUnscheduledItemsGrouped(
            @RequestHeader("X-User-Id") Long userId) {

        try {
            log.info("Getting unscheduled items for userId={}", userId);

            BaseResponse<?> response = unscheduledItemsService.getUnscheduledItemsGroupedByMonth(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get unscheduled items for userId={}", userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve unscheduled items", null)
            );
        }
    }
}