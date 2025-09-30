package com.graduation.userservice.repository;

import com.graduation.userservice.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByResetTokenAndIsUsedFalse(String resetToken);

    Optional<PasswordResetToken> findByUserIdAndIsUsedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.resetToken = :token")
    void markAsUsed(@Param("token") String token);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.userId = :userId")
    void invalidateAllUserTokens(@Param("userId") Long userId);
}