package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invitation_tokens", indexes = {
        @Index(name = "idx_invitation_token", columnList = "token"),
        @Index(name = "idx_invitation_user_project", columnList = "userId,projectId"),
        @Index(name = "idx_invitation_expires_at", columnList = "expiresAt")
})
public class InvitationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- HELPER METHODS ---

    private static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static InvitationToken create(Long userId, Long senderId, Long projectId, int expirationDays) {
        InvitationToken token = new InvitationToken();
        token.setUserId(userId);
        token.setSenderId(senderId);

        token.setProjectId(projectId);
        token.setToken(generateSecureToken());
        token.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        return token;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void markAsUsed() {
        this.isUsed = true;
    }
}
