package com.graduation.userservice.repository;

import com.graduation.userservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);
    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId AND s.sessionId <> :currentSessionId")
    void invalidateAllOtherUserSessions(Long userId, String currentSessionId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.sessionId = :sessionId")
    void invalidateSession(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void invalidateAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :accessTime WHERE s.sessionId = :sessionId")
    void updateLastAccessed(@Param("sessionId") String sessionId, @Param("accessTime") LocalDateTime accessTime);
}