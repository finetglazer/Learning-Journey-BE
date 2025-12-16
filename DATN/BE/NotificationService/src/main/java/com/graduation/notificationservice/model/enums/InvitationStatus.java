package com.graduation.notificationservice.model.enums;

/**
 * Enum representing the status of an invitation notification.
 * Only applicable when NotificationType is ACTION_INVITATION.
 */
public enum InvitationStatus {
    /**
     * Default status for non-invitation notifications.
     */
    NONE,

    /**
     * Invitation has been sent but not yet responded to.
     */
    PENDING,

    /**
     * Invitation has been accepted by the recipient.
     */
    ACCEPTED,

    /**
     * Invitation has been declined by the recipient.
     */
    DECLINED,

    /**
     * Invitation has expired and can no longer be accepted.
     */
    EXPIRED
}
