package com.graduation.userservice.repository;

import com.graduation.userservice.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByVerificationCodeAndIsUsedFalse(String verificationCode);
    Optional<EmailVerification> findByUserIdAndIsUsedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailVerification e SET e.isUsed = true WHERE e.verificationCode = :code")
    void markAsUsed(@Param("code") String verificationCode);
}
