package com.graduation.notificationservice.listener;

import com.graduation.notificationservice.config.KafkaConfig;
import com.graduation.notificationservice.event.UserUpdatedEvent;
import com.graduation.notificationservice.model.ProcessedMessage;
import com.graduation.notificationservice.model.ProcessedMessage.ProcessStatus;
import com.graduation.notificationservice.model.UserInfoCache;
import com.graduation.notificationservice.repository.ProcessedMessageRepository;
import com.graduation.notificationservice.repository.UserInfoCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka listener for user profile update events.
 * Updates the local UserInfoCache when user profiles change in UserService.
 * Implements idempotency using ProcessedMessage tracking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileKafkaListener {

    private final UserInfoCacheRepository userInfoCacheRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    /**
     * Handle user updated events from Kafka.
     * Updates or creates a UserInfoCache entry for the updated user.
     *
     * @param record The Kafka consumer record
     * @param ack    Acknowledgment for manual commit
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_USER_UPDATED, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUpdatedEvent(ConsumerRecord<String, UserUpdatedEvent> record, Acknowledgment ack) {
        String messageId = generateMessageId(record);
        UserUpdatedEvent event = record.value();

        log.info("Received UserUpdatedEvent for userId={}, messageId={}",
                event.getUserId(), messageId);

        try {
            // Idempotency check - skip if already processed
            if (processedMessageRepository.existsByMessageId(messageId)) {
                log.info("Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            // Update UserInfoCache
            UserInfoCache cache = userInfoCacheRepository.findById(event.getUserId())
                    .orElse(new UserInfoCache());

            cache.setUserId(event.getUserId());
            cache.setDisplayName(event.getDisplayName());
            cache.setAvatarUrl(event.getAvatarUrl());

            userInfoCacheRepository.save(cache);

            // Mark message as processed
            ProcessedMessage processedMessage = new ProcessedMessage(
                    messageId,
                    record.topic(),
                    Instant.now(),
                    ProcessStatus.SUCCESS);
            processedMessageRepository.save(processedMessage);

            log.info("Successfully updated UserInfoCache for userId={}", event.getUserId());

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process UserUpdatedEvent for userId={}: {}",
                    event.getUserId(), e.getMessage());

            // Save failed status for tracking
            try {
                ProcessedMessage failedMessage = new ProcessedMessage(
                        messageId,
                        record.topic(),
                        Instant.now(),
                        ProcessStatus.FAILED);
                processedMessageRepository.save(failedMessage);
            } catch (Exception saveEx) {
                log.error("Failed to save failed message status: {}", saveEx.getMessage());
            }

            // Re-throw to trigger retry/DLQ
            throw e;
        }
    }

    /**
     * Generate a unique message ID from the Kafka record.
     * Uses topic-partition-offset as a unique identifier.
     */
    private String generateMessageId(ConsumerRecord<String, ?> record) {
        return String.format("%s-%d-%d",
                record.topic(),
                record.partition(),
                record.offset());
    }
}
