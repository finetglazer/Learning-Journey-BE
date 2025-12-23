package com.graduation.notificationservice.controller;

import com.graduation.notificationservice.payload.request.UpdateReadStatusRequest;
import com.graduation.notificationservice.payload.response.BaseResponse;
import com.graduation.notificationservice.service.NotificationService;
import com.graduation.notificationservice.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller for notification-related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    /**
     * Create SSE connection
     */
    @GetMapping(path = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable Long userId) {
        return sseService.createConnection(userId);
    }

    /**
     * Lightweight health check for the frontend probe.
     * Used when the SSE stream is broken to decide when to reconnect.
     */
    @GetMapping("/health-check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Retrieve notifications for the authenticated user.
     * Supports filtering by read status and pagination.
     *
     * @param filter Filter type: "UNREAD" for inbox tab, "ALL" for all tab
     *               (default: "ALL")
     * @param page   Page number, 1-indexed (default: 1)
     * @param limit  Number of notifications per page (default: 50)
     * @return Response containing notification list and pagination metadata
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("GET /api/notifications - userId={}, filter={}, page={}, limit={}", userId, filter, page, limit);

        BaseResponse<?> response = notificationService.getNotifications(userId, filter, page, limit);

        return ResponseEntity.ok(response);
    }

    /**
     * Update the read status of a specific notification.
     *
     * @param userId         Authenticated user ID
     * @param notificationId ID of the notification to update
     * @param request        Request body containing isRead status
     * @return Response with success message
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> updateReadStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId,
            @RequestBody @Valid UpdateReadStatusRequest request) {

        log.info("PATCH /api/notifications/{}/read - userId={}, isRead={}",
                notificationId, userId, request.getIsRead());

        BaseResponse<?> response = notificationService.updateReadStatus(
                userId, notificationId, request.getIsRead());

        return ResponseEntity.ok(response);
    }

    /**
     * Mark all unread notifications as read for the authenticated user.
     *
     * @param userId Authenticated user ID
     * @return Response with success message
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("PUT /api/notifications/mark-all-read - userId={}", userId);

        BaseResponse<?> response = notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete all read notifications for the authenticated user.
     *
     * @param userId Authenticated user ID
     * @return Response with success message
     */
    @DeleteMapping("/read")
    public ResponseEntity<?> clearReadNotifications(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("DELETE /api/notifications/read - userId={}", userId);

        BaseResponse<?> response = notificationService.clearReadNotifications(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a specific notification.
     *
     * @param userId         Authenticated user ID
     * @param notificationId ID of the notification to delete
     * @return Response with success message
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId) {

        log.info("DELETE /api/notifications/{} - userId={}", notificationId, userId);

        BaseResponse<?> response = notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity.ok(response);
    }
}
