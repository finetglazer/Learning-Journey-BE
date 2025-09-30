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
@Table(name = Constant.TABLE_USER_SESSIONS,
        indexes = {
                @Index(name = "idx_user_session_id", columnList = "sessionId"),
                @Index(name = "idx_user_session_user_id", columnList = "userId"),
                @Index(name = "idx_user_session_last_accessed", columnList = "lastAccessedAt"),
                @Index(name = "idx_user_session_is_active", columnList = "isActive")
        })
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 255)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public static UserSession create(Long userId, String sessionId) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        return session;
    }

    public void refresh() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void invalidate() {
        this.isActive = false;
    }

    public boolean isValid() {
        return this.isActive;
    }
}