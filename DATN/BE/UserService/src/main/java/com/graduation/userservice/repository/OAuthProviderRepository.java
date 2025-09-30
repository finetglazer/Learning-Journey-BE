package com.graduation.userservice.repository;

import com.graduation.userservice.model.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, Long> {

    Optional<OAuthProvider> findByProviderAndProviderIdAndIsActiveTrue(
            OAuthProvider.ProviderType provider, String providerId);

    List<OAuthProvider> findByUserIdAndIsActiveTrue(Long userId);

    Optional<OAuthProvider> findByUserIdAndProviderAndIsActiveTrue(
            Long userId, OAuthProvider.ProviderType provider);

    boolean existsByProviderAndProviderIdAndIsActiveTrue(
            OAuthProvider.ProviderType provider, String providerId);

    /**
     * Checks if an active link exists between a user and a specific provider.
     * This is used in the OAuth2 success handler to prevent creating duplicate links.
     *
     * @param userId The ID of the user.
     * @param provider The OAuth provider (e.g., GOOGLE).
     * @return true if an active link exists, false otherwise.
     */
    boolean existsByUserIdAndProviderAndIsActiveTrue(Long userId, OAuthProvider.ProviderType provider);

    @Modifying
    @Query("UPDATE OAuthProvider o SET o.isActive = false WHERE o.userId = :userId AND o.provider = :provider")
    void unlinkProvider(@Param("userId") Long userId, @Param("provider") OAuthProvider.ProviderType provider);
}