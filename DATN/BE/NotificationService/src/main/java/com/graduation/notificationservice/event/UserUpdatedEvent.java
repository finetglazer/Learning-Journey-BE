package com.graduation.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event received when a user profile is updated.
 * Used for async communication from UserService to sync user cache data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID of the updated user.
     */
    private Long userId;

    /**
     * User's display name.
     */
    private String displayName;

    /**
     * URL to user's avatar image (nullable).
     */
    private String avatarUrl;

    /**
     * Timestamp when the update occurred.
     */
    private LocalDateTime updatedAt;
}
