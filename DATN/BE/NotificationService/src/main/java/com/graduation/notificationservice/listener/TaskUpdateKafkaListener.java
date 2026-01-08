package com.graduation.notificationservice.listener;

import com.graduation.notificationservice.config.KafkaConfig;
import com.graduation.notificationservice.event.TaskUpdateEvent;
import com.graduation.notificationservice.model.Notification;
import com.graduation.notificationservice.model.ProcessedMessage;
import com.graduation.notificationservice.model.enums.NotificationType;
import com.graduation.notificationservice.repository.NotificationRepository;
import com.graduation.notificationservice.repository.ProcessedMessageRepository;
import com.graduation.notificationservice.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.graduation.notificationservice.client.UserServiceClient;
import com.graduation.notificationservice.model.UserInfoCache;
import com.graduation.notificationservice.payload.response.NotificationDTO;
import com.graduation.notificationservice.payload.response.SenderDTO;
import com.graduation.notificationservice.payload.response.UserBatchDTO;
import com.graduation.notificationservice.repository.UserInfoCacheRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskUpdateKafkaListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final SseService sseService;
    private final UserInfoCacheRepository userInfoCacheRepository;
    private final UserServiceClient userServiceClient;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @KafkaListener(topics = KafkaConfig.TOPIC_PROJECT_TASK_UPDATE, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "taskUpdateKafkaListenerContainerFactory")
    public void handleTaskUpdateEvent(ConsumerRecord<String, TaskUpdateEvent> record, Acknowledgment ack) {
        String messageId = generateMessageId(record);
        TaskUpdateEvent event = record.value();

        log.info("Received TaskUpdateEvent: {}", event);

        try {
            // Idempotency check
            if (processedMessageRepository.existsByMessageId(messageId)) {
                log.info("Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            for (Long assigneeId : event.getAssigneeIds()) {
                // Do not notify the user who performed the update
                if (assigneeId.equals(event.getUpdatedBy())) {
                    continue;
                }

                createTaskUpdateNotification(assigneeId, event);
            }

            // Mark processed
            processedMessageRepository.save(new ProcessedMessage(
                    messageId, record.topic(), Instant.now(), ProcessedMessage.ProcessStatus.SUCCESS));

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process task update event: {}", e.getMessage(), e);
            ack.acknowledge(); // Acknowledge to avoid infinite loops, or handle DLT if configured
        }
    }

    private void createTaskUpdateNotification(Long recipientId, TaskUpdateEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setSenderId(event.getUpdatedBy());
        String message = switch (event.getAction() != null ? event.getAction() : TaskUpdateEvent.ACTION_UPDATE) {
            case TaskUpdateEvent.ACTION_COMMENT_ADD -> "A task you are assigned has a new comment";
            case TaskUpdateEvent.ACTION_COMMENT_UPDATE -> "A task you are assigned has a modified comment";
            case TaskUpdateEvent.ACTION_COMMENT_DELETE -> "A comment was deleted on a task you are assigned";
            default -> "A task you are assigned is updated";
        };
        notification.setContentMessage(message);
        notification.setType(NotificationType.NAVIGATE_VIEW);
        notification.setReferenceId(event.getProjectId());
        notification.setTargetUrl("/projects/" + event.getProjectId() + "?tab=list&taskId=" + event.getTaskId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        // Status is NONE (null) or based on defaults

        Notification savedNotification = notificationRepository.save(notification);

        // Send via SSE
        sseService.sendToUser(recipientId, toNotificationDTO(savedNotification));
    }

    private NotificationDTO toNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getNotificationId());

        // Map sender information
        SenderDTO sender = mapSender(notification.getSenderId());
        dto.setSender(sender);

        dto.setContentMessage(notification.getContentMessage());
        dto.setType(notification.getType().name());
        dto.setTargetUrl(notification.getTargetUrl());

        if (notification.getInvitationStatus() != null) {
            dto.setInvitationStatus(notification.getInvitationStatus().name());
        }

        dto.setIsRead(notification.getIsRead());
        dto.setToken(notification.getToken());
        dto.setReferenceId(notification.getReferenceId());

        // Format createdAt to ISO 8601 string
        dto.setCreatedAt(notification.getCreatedAt().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER));

        return dto;
    }

    private SenderDTO mapSender(Long senderId) {
        if (senderId == null) {
            return new SenderDTO(null, "System", null);
        }

        Optional<UserInfoCache> optionalUserInfoCache = userInfoCacheRepository.findById(senderId);

        if (optionalUserInfoCache.isPresent()) {
            UserInfoCache userInfoCache = optionalUserInfoCache.get();
            return new SenderDTO(
                    userInfoCache.getUserId(),
                    userInfoCache.getDisplayName(),
                    userInfoCache.getAvatarUrl());
        }

        // Sender not found in cache - call UserService to fetch and cache user info
        log.info("Sender userId={} not found in cache, fetching from UserService", senderId);

        // Use UserServiceClient to fetch user info
        Optional<UserBatchDTO> userOptional = userServiceClient.findById(senderId);

        if (userOptional.isPresent()) {
            UserBatchDTO userBatchDTO = userOptional.get();
            log.info("Successfully fetched user info for userId={} from UserService", senderId);

            // Cache the user info
            UserInfoCache newCache = new UserInfoCache();
            newCache.setUserId(userBatchDTO.getUserId());
            newCache.setDisplayName(userBatchDTO.getName());
            newCache.setAvatarUrl(userBatchDTO.getAvatarUrl());
            userInfoCacheRepository.save(newCache);
            log.info("Cached user info for userId={}", senderId);

            return new SenderDTO(
                    userBatchDTO.getUserId(),
                    userBatchDTO.getName(),
                    userBatchDTO.getAvatarUrl());
        } else {
            log.warn("User not found in UserService for userId={}", senderId);
            return new SenderDTO(senderId, "Unknown User", null);
        }
    }

    private String generateMessageId(ConsumerRecord<String, ?> record) {
        return String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
    }
}
