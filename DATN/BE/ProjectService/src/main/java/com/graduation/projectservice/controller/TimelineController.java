package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.request.DeleteTimelineRequest;
import com.graduation.projectservice.payload.request.GetTimelineStructureRequest;
import com.graduation.projectservice.payload.request.UpdateTimelineDatesRequest;
import com.graduation.projectservice.payload.request.UpdateTimelineOffsetRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.TimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    // --- New Endpoint ---
    @PostMapping("/structure")
    public ResponseEntity<BaseResponse<?>> getTimelineStructure(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestBody GetTimelineStructureRequest request) {

        BaseResponse<?> response = timelineService.getTimelineStructure(userId, projectId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/dates")
    public ResponseEntity<BaseResponse<?>> updateTimelineDates(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateTimelineDatesRequest request) {

        log.info("Timeline date update requested. Project: {}, User: {}, Type: {}",
                projectId, userId, request.getType());

        BaseResponse<?> response = timelineService.updateTimelineDates(userId, projectId, request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/offset")
    public ResponseEntity<BaseResponse<?>> offsetTimelineItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateTimelineOffsetRequest request) {

        log.info("Timeline offset requested. Project: {}, User: {}, Type: {}, Days: {}",
                projectId, userId, request.getType(), request.getOffsetDays());

        BaseResponse<?> response = timelineService.offsetTimelineItem(userId, projectId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("")
    public ResponseEntity<BaseResponse<?>> deleteTimelineItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody DeleteTimelineRequest request) {

        BaseResponse<?> response = timelineService.deleteTimeline(userId, projectId, request);

        return ResponseEntity.ok(response);
    }
}