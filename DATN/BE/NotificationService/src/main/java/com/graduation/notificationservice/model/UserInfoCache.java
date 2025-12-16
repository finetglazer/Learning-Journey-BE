package com.graduation.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cached user information for displaying notification senders.
 * Purpose:
 * 1. Cache user display data to avoid calling UserService on every notification
 * retrieval
 * 2. Future: Sync with UserService when user not found in cache
 * 3. Future: Update via RabbitMQ when user profile changes
 */
@Entity
@Table(name = "user_info_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoCache {

    /**
     * User ID - matches the user_id in UserService.
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * User's display name.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * URL to user's avatar image.
     * Can be null if user has no avatar.
     */
    @Column(name = "avatar_url")
    private String avatarUrl;
}
