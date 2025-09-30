package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = Constant.TABLE_PASSWORD_RESET_TOKENS,
        indexes = {
                @Index(name = "idx_password_reset_token", columnList = "resetToken"),
                @Index(name = "idx_password_reset_user_id", columnList = "userId"),
                @Index(name = "idx_password_reset_expires_at", columnList = "expiresAt")
        })
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reset_token", nullable = false, unique = true, length = 100)
    private String resetToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    public static PasswordResetToken generate(Long userId, int expirationHours) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setResetToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        return token;
    }

    public boolean validate() {
        return !isUsed && !isExpired();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void invalidate() {
        this.isUsed = true;
    }
}