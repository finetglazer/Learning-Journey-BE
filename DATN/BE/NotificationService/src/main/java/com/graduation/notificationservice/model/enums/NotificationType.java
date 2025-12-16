package com.graduation.notificationservice.model.enums;

/**
 * Enum representing the type of notification.
 * Determines how the frontend should render the notification.
 */
public enum NotificationType {
    /**
     * Simple informational notification with no action required.
     * Frontend displays as read-only text.
     */
    INFO_ONLY,

    /**
     * Notification with a "View" button.
     * Contains a targetUrl that the user can navigate to.
     */
    NAVIGATE_VIEW,

    /**
     * Invitation notification with Accept/Decline action buttons.
     * Contains invitationStatus to track the user's response.
     */
    ACTION_INVITATION
}
