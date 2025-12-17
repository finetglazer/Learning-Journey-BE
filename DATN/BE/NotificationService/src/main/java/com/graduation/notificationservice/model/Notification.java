package com.graduation.notificationservice.model;

import com.graduation.notificationservice.model.enums.InvitationStatus;
import com.graduation.notificationservice.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a notification sent to a user.
 * Notifications can be informational, navigational, or action-based
 * (invitations).
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    /**
     * ID of the user who will receive this notification.
     */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /**
     * ID of the user who triggered/sent this notification.
     * Can be null for system-generated notifications.
     */
    @Column(name = "sender_id")
    private Long senderId;

    /**
     * The notification message content.
     * E.g., "sent you an invitation to the LSD project"
     */
    @Column(name = "content_message", nullable = false, columnDefinition = "TEXT")
    private String contentMessage;

    /**
     * Type of notification - determines how the frontend renders it.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    /**
     * URL to navigate to when user clicks "View" button.
     * Only applicable for NAVIGATE_VIEW type.
     */
    @Column(name = "target_url")
    private String targetUrl;

    /**
     * Reference ID to the related entity (e.g., project_id, task_id).
     * Used for tracking and potential cleanup.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Invitation token (for ACTION_INVITATION type).
     */
    @Column(name = "token")
    private String token;

    /**
     * Status of the invitation.
     * Only applicable for ACTION_INVITATION type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false)
    private InvitationStatus invitationStatus = InvitationStatus.NONE;

    /**
     * Whether the notification has been read by the recipient.
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * Timestamp when the notification was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
