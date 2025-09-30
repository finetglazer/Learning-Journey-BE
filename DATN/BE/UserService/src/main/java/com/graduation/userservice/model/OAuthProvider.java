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
@Table(name = Constant.TABLE_OAUTH_PROVIDERS,
        indexes = {
                @Index(name = "idx_oauth_user_id", columnList = "userId"),
                @Index(name = "idx_oauth_provider", columnList = "provider"),
                @Index(name = "idx_oauth_provider_id", columnList = "providerId"),
                @Index(name = "idx_oauth_provider_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_provider_user",
                        columnNames = {"provider", "providerId"})
        })
public class OAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public static OAuthProvider linkAccount(Long userId, ProviderType provider,
                                            String providerId, String email) {
        OAuthProvider oAuth = new OAuthProvider();
        oAuth.setUserId(userId);
        oAuth.setProvider(provider);
        oAuth.setProviderId(providerId);
        oAuth.setEmail(email);
        return oAuth;
    }

    public void unlinkAccount() {
        this.isActive = false;
    }

    public boolean isLinked() {
        return this.isActive;
    }

    public enum ProviderType {
        GOOGLE,
        FACEBOOK,
        GITHUB,
        MICROSOFT
    }
}