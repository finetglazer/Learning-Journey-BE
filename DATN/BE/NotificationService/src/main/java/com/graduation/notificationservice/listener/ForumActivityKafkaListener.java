package com.graduation.notificationservice.listener;

import com.graduation.notificationservice.config.KafkaConfig;
import com.graduation.notificationservice.event.ForumActivityEvent;
import com.graduation.notificationservice.model.Notification;
import com.graduation.notificationservice.model.ProcessedMessage;
import com.graduation.notificationservice.model.enums.NotificationType;
import com.graduation.notificationservice.payload.response.NotificationDTO;
import com.graduation.notificationservice.payload.response.SenderDTO;
import com.graduation.notificationservice.repository.NotificationRepository;
import com.graduation.notificationservice.repository.ProcessedMessageRepository;
import com.graduation.notificationservice.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForumActivityKafkaListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final SseService sseService;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @KafkaListener(topics = KafkaConfig.TOPIC_FORUM_ACTIVITY, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "forumActivityKafkaListenerContainerFactory")
    public void handleForumEvent(ConsumerRecord<String, ForumActivityEvent> record, Acknowledgment ack) {
        String messageId = generateMessageId(record);
        ForumActivityEvent event = record.value();

        log.info("Received ForumActivityEvent: {}", event);

        try {
            // Idempotency check
            if (processedMessageRepository.existsByMessageId(messageId)) {
                log.info("Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            // Ensure we don't notify the person who performed the action
            if (!event.getActorId().equals(event.getRecipientId())) {
                createForumNotification(event);
            }

            // Mark processed
            processedMessageRepository.save(new ProcessedMessage(
                    messageId, record.topic(), Instant.now(), ProcessedMessage.ProcessStatus.SUCCESS));

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process forum event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    private void createForumNotification(ForumActivityEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.getRecipientId());
        notification.setSenderId(event.getActorId());

        // Standardized Target URL for all cases
        String targetUrl = "/posts/" + event.getPostId();

        String message = switch (event.getType()) {
            case ANSWER_ON_POST -> event.getActorName() + " answered your question: " + event.getPostTitle();
            case COMMENT_ON_POST -> event.getActorName() + " commented on your post: " + event.getPostTitle();
            case COMMENT_ON_ANSWER -> event.getActorName() + " commented on your answer in: " + event.getPostTitle();
            case REPLY_ON_COMMENT -> event.getActorName() + " replied to your comment in: " + event.getPostTitle();
            default -> "New activity on post: " + event.getPostTitle();
        };

        notification.setContentMessage(message);
        notification.setType(NotificationType.NAVIGATE_VIEW);
        notification.setReferenceId(event.getPostId());
        notification.setTargetUrl(targetUrl);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        // Convert to DTO and Send via SSE
        // Pass 'event' to utilize the actor details directly
        NotificationDTO notificationDTO = toNotificationDTO(savedNotification, event);
        sseService.sendToUser(event.getRecipientId(), notificationDTO);
    }

    /**
     * Converts Entity to DTO using the Actor info from the Event.
     * This avoids calling User Service or Cache because the Event already has the latest Name/Avatar.
     */
    private NotificationDTO toNotificationDTO(Notification notification, ForumActivityEvent event) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getNotificationId());

        // OPTIMIZATION: Construct SenderDTO directly from Event data
        // No need to query DB/Cache
        SenderDTO sender = new SenderDTO(
                event.getActorId(),
                event.getActorName(),
                event.getActorAvatarUrl()
        );
        dto.setSender(sender);

        dto.setContentMessage(notification.getContentMessage());
        dto.setType(notification.getType().name());
        dto.setTargetUrl(notification.getTargetUrl());

        // BUG FIX: Check for null before accessing .name()
        if (notification.getInvitationStatus() != null) {
            dto.setInvitationStatus(notification.getInvitationStatus().name());
        }

        dto.setIsRead(notification.getIsRead());
        dto.setToken(notification.getToken());
        dto.setReferenceId(notification.getReferenceId());

        // Format createdAt to ISO 8601 string
        dto.setCreatedAt(notification.getCreatedAt().format(ISO_FORMATTER));

        return dto;
    }

    private String generateMessageId(ConsumerRecord<String, ?> record) {
        return String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
    }
}