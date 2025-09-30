package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = Constant.TABLE_USERS,
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_display_name", columnList = "displayName")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = true, length = 255) // nullable for OAuth-only accounts
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false; // false until email verification

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "preferred_timezone", length = 50)
    private String preferredTimezone = "UTC";

    // Auth-related methods
    public void activate() {
        this.isActive = true;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean hasPassword() {
        return this.passwordHash != null && !this.passwordHash.trim().isEmpty();
    }
}