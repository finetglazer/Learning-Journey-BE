package com.graduation.userservice.repository;

import com.graduation.userservice.model.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {

    Optional<InvitationToken> findByTokenAndIsUsedFalse(String token);

    Optional<InvitationToken> findByUserIdAndProjectIdAndIsUsedFalse(Long userId, Long projectId);
}