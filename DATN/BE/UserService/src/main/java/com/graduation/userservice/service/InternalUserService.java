package com.graduation.userservice.service;

import com.graduation.userservice.payload.response.UserBatchDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InternalUserService {

    Optional<UserBatchDTO> findUserByEmail(String email);

    List<UserBatchDTO> findUsersByEmail(String email);

    List<UserBatchDTO> findUsersByIds(List<Long> userIds);

    String createInvitationToken(Long userId, Long projectId, String projectName);

    /**
     * Validate invitation token and return user/project info
     * @param token the invitation token
     * @return Map with userId and projectId if valid, empty if invalid
     */
    Optional<Map<String, Long>> validateInvitationToken(String token);
}