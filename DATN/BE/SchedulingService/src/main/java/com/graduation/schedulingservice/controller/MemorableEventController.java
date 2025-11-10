package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.payload.request.UpdateMemorableEventsRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.service.MemorableEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/calendar/memorable-events")
@RequiredArgsConstructor
public class MemorableEventController {

    private final MemorableEventService memorableEventService;

    /**
     * Retrieve all memorable events configured by the user
     *
     * @param userId Extracted from X-User-Id header
     * @return Response containing all memorable events
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getMemorableEvents(
            @RequestHeader("X-User-Id") Long userId) {

        try {
            log.info("Getting memorable events: userId={}", userId);

            BaseResponse<?> response = memorableEventService.getMemorableEvents(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get memorable events: userId={}", userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to retrieve memorable events", null)
            );
        }
    }

    /**
     * Replace entire list of memorable events
     * Deletes old events and calendar items, creates new ones
     * Auto-generates calendar Event items for next 5 years
     *
     * @param userId Extracted from X-User-Id header
     * @param request The request containing the new list of memorable events
     * @return Response containing the number of events created and calendar items generated
     */
    @PutMapping
    public ResponseEntity<BaseResponse<?>> updateMemorableEvents(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateMemorableEventsRequest request) {

        try {
            log.info("Updating memorable events: userId={}, eventCount={}",
                    userId, request.getEvents().size());

            BaseResponse<?> response = memorableEventService.updateMemorableEvents(userId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update memorable events: userId={}", userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, "Failed to update memorable events", null)
            );
        }
    }
}