package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = Constant.TABLE_EMAIL_VERIFICATIONS,
        indexes = {
                @Index(name = "idx_email_verification_code", columnList = "verificationCode"),
                @Index(name = "idx_email_verification_user_id", columnList = "userId"),
                @Index(name = "idx_email_verification_expires_at", columnList = "expiresAt")
        })
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "verification_code", nullable = false, length = 6) // Length can be shorter now
    private String verificationCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // The temporary fields below are no longer needed with the current registration flow
    // @Column(name = "temp_email", length = 255)
    // private String tempEmail;
    //
    // @Column(name = "temp_display_name", length = 100)
    // private String tempDisplayName;
    //
    // @Column(name = "temp_password_hash", length = 255)
    // private String tempPasswordHash;

    // --- HELPER METHODS ---

    private static String generateSixDigitCode() {
        SecureRandom random = new SecureRandom();
        // Generate a number between 100000 and 999999
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }

    public static EmailVerification createForExistingUser(Long userId, int expirationHours) {
        EmailVerification verification = new EmailVerification();
        verification.setUserId(userId);
        verification.setVerificationCode(generateSixDigitCode()); // Use the new 6-digit code
        verification.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        return verification;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void markAsUsed() {
        this.isUsed = true;
    }
}