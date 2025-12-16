package com.graduation.notificationservice.service;

import com.graduation.notificationservice.payload.response.BaseResponse;

/**
 * Service interface for notification operations.
 */
public interface NotificationService {

    /**
     * Retrieve notifications for a user with filtering and pagination.
     *
     * @param userId ID of the user requesting notifications
     * @param filter Filter type: "UNREAD" for unread notifications only, "ALL" for
     *               all notifications
     * @param page   Page number (1-indexed)
     * @param limit  Number of notifications per page (default: 50)
     * @return BaseResponse containing notification list and pagination metadata
     */
    BaseResponse<?> getNotifications(Long userId, String filter, int page, int limit);

    /**
     * Update the read status of a specific notification.
     * Verifies that the user owns the notification.
     *
     * @param userId         ID of the user making the request
     * @param notificationId ID of the notification to update
     * @param isRead         New read status (true or false)
     * @return BaseResponse with success message
     */
    BaseResponse<?> updateReadStatus(Long userId, Long notificationId, Boolean isRead);

    /**
     * Mark all unread notifications as read for a user.
     *
     * @param userId ID of the user
     * @return BaseResponse with success message
     */
    BaseResponse<?> markAllAsRead(Long userId);

    /**
     * Delete all read notifications for a user.
     *
     * @param userId ID of the user
     * @return BaseResponse with success message
     */
    BaseResponse<?> clearReadNotifications(Long userId);

    /**
     * Delete a specific notification.
     * Verifies that the user owns the notification.
     *
     * @param userId         ID of the user making the request
     * @param notificationId ID of the notification to delete
     * @return BaseResponse with success message
     */
    BaseResponse<?> deleteNotification(Long userId, Long notificationId);
}
