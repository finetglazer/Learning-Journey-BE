package com.graduation.userservice.event;

import com.graduation.userservice.config.KafkaConfig;
import com.graduation.userservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for publishing user profile events to Kafka.
 * Uses fire-and-forget pattern for async communication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a user updated event to Kafka.
     * This is an async operation - the calling code does not wait for consumers.
     *
     * @param user The updated user entity
     */
    public void publishUserUpdatedEvent(User user) {
        try {
            // Generate unique message ID for idempotency
            String messageId = UUID.randomUUID().toString();

            UserUpdatedEvent event = new UserUpdatedEvent(
                    user.getId(),
                    user.getDisplayName(),
                    user.getAvatarUrl(),
                    LocalDateTime.now());

            // Use userId as key for partition ordering
            String key = String.valueOf(user.getId());

            kafkaTemplate.send(KafkaConfig.TOPIC_USER_UPDATED, key, event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to publish UserUpdatedEvent for userId={}: {}",
                                    user.getId(), throwable.getMessage());
                        } else {
                            log.info("Published UserUpdatedEvent for userId={} to partition={} offset={}",
                                    user.getId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            // Log error but don't fail the main operation
            // The profile update should succeed even if event publishing fails
            log.error("Failed to publish UserUpdatedEvent for userId={}: {}", user.getId(), e.getMessage());
        }
    }
}
