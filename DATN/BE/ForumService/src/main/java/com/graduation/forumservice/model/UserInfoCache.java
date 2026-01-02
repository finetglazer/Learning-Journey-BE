package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_info_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoCache {
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

    @Column(name = "email")
    private String email;
}
