package com.graduation.userservice.service.impl;

import com.graduation.userservice.event.InvitationSentEvent;
import com.graduation.userservice.model.InvitationToken;
import com.graduation.userservice.model.User;
import com.graduation.userservice.payload.response.UserBatchDTO;
import com.graduation.userservice.repository.InvitationTokenRepository;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.EmailService;
import com.graduation.userservice.service.InternalUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InternalUserServiceImpl implements InternalUserService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final EmailService emailService;

    @Override
    public Optional<UserBatchDTO> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new UserBatchDTO(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getAvatarUrl()
                ));
    }

    @Override
    public List<UserBatchDTO> findUsersByIds(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                .map(user -> new UserBatchDTO(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getAvatarUrl()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String createInvitationToken(Long userId, Long projectId, String projectName) {
        // Check if there's an existing unused token
        Optional<InvitationToken> existingToken =
                invitationTokenRepository.findByUserIdAndProjectIdAndIsUsedFalse(userId, projectId);

        InvitationToken token;
        if (existingToken.isPresent() && !existingToken.get().isExpired()) {
            // Reuse existing token if not expired
            token = existingToken.get();
            log.info("Reusing existing invitation token for user {} to project {}", userId, projectId);
        } else {
            // Create new token (expires in 7 days)
            token = InvitationToken.create(userId, projectId, 7);
            token = invitationTokenRepository.save(token);
            log.info("Created new invitation token for user {} to project {}", userId, projectId);
        }

        // Get user email
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Publish event for async email sending
        eventPublisher.publishEvent(new InvitationSentEvent(
                this,
                user.getEmail(),
                user.getDisplayName(),
                projectName,
                token.getToken()
        ));

        log.info("Invitation event published for user {} to project {}", userId, projectId);

        return token.getToken();
    }

    @Override
    @Transactional
    public Optional<Map<String, Long>> validateInvitationToken(String token) {
        // Find token
        Optional<InvitationToken> tokenOpt = invitationTokenRepository.findByTokenAndIsUsedFalse(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Token not found or already used: {}", token);
            return Optional.empty();
        }

        InvitationToken invitationToken = tokenOpt.get();

        // Check if expired
        if (invitationToken.isExpired()) {
            log.warn("Token expired: {}", token);
            return Optional.empty();
        }

        // Mark token as used
        invitationToken.markAsUsed();
        invitationTokenRepository.save(invitationToken);

        log.info("Token validated and marked as used for user {} project {}",
                invitationToken.getUserId(), invitationToken.getProjectId());

        return Optional.of(Map.of(
                "userId", invitationToken.getUserId(),
                "projectId", invitationToken.getProjectId()
        ));
    }
}