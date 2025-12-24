package com.graduation.notificationservice.listener;

import com.graduation.notificationservice.client.UserServiceClient;
import com.graduation.notificationservice.config.KafkaConfig;
import com.graduation.notificationservice.event.ProjectInvitationEvent;
import com.graduation.notificationservice.model.Notification;
import com.graduation.notificationservice.model.ProcessedMessage;
import com.graduation.notificationservice.model.UserInfoCache;
import com.graduation.notificationservice.model.enums.InvitationStatus;
import com.graduation.notificationservice.model.enums.NotificationType;
import com.graduation.notificationservice.payload.response.NotificationDTO;
import com.graduation.notificationservice.payload.response.SenderDTO;
import com.graduation.notificationservice.payload.response.UserBatchDTO;
import com.graduation.notificationservice.repository.NotificationRepository;
import com.graduation.notificationservice.repository.ProcessedMessageRepository;
import com.graduation.notificationservice.repository.UserInfoCacheRepository;
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
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationKafkaListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final UserInfoCacheRepository userInfoCacheRepository;
    private final SseService sseService;
    private final UserServiceClient userServiceClient;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @KafkaListener(topics = KafkaConfig.TOPIC_PROJECT_INVITATION, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "invitationKafkaListenerContainerFactory")
    public void handleInvitationEvent(ConsumerRecord<String, ProjectInvitationEvent> record, Acknowledgment ack) {
        String messageId = generateMessageId(record);
        ProjectInvitationEvent event = record.value();

        log.info("Received ProjectInvitationEvent: {}", event);

        try {
            // Idempotency check
            if (processedMessageRepository.existsByMessageId(messageId)) {
                log.info("Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            if (event.getIsExpired()) {
                createInvitationExpiredNotification(event);
            }
            if (event.getIsAccepted() == null) {
                // Invitation Sent -> Notify Recipient
                createInvitationNotification(event);
            } else if (event.getIsAccepted()) {
                // Invitation Accepted -> Notify Sender
                createAcceptedNotification(event);
            } else {
                // Invitation Declined -> Notify Sender
                createDeclinedNotification(event);
            }

            // Mark processed
            processedMessageRepository.save(new ProcessedMessage(
                    messageId, record.topic(), Instant.now(), ProcessedMessage.ProcessStatus.SUCCESS));

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process invitation event: {}", e.getMessage(), e);
            // Save fail status?
            try {
                processedMessageRepository.save(new ProcessedMessage(
                        messageId, record.topic(), Instant.now(), ProcessedMessage.ProcessStatus.FAILED));
            } catch (Exception ex) {
                /* ignore */
            }
            throw e; // Retry
        }
    }

    private void createInvitationExpiredNotification(ProjectInvitationEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.getRecipientId());
        notification.setSenderId(event.getSenderId());
        notification.setContentMessage("Your invitation to project " + event.getProjectName() + " has been expired");
        notification.setType(NotificationType.INFO_ONLY);
        notification.setReferenceId(event.getProjectId());
        notification.setToken(null);
        notification.setInvitationStatus(InvitationStatus.EXPIRED);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        sseService.sendToUser(event.getRecipientId(), notification);

        notificationRepository.save(notification);

        changeInvitationStatus(event.getToken(), InvitationStatus.EXPIRED);
    }

    private void createInvitationNotification(ProjectInvitationEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.getRecipientId());
        notification.setSenderId(event.getSenderId());
        notification.setContentMessage("You have been invited to join project " + event.getProjectName());
        notification.setType(NotificationType.ACTION_INVITATION);
        notification.setReferenceId(event.getProjectId());
        notification.setToken(event.getToken());
        notification.setInvitationStatus(InvitationStatus.PENDING);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        sseService.sendToUser(savedNotification.getRecipientId(), toNotificationDTO(savedNotification));
    }

    private void createAcceptedNotification(ProjectInvitationEvent event) {
        String userName = userInfoCacheRepository.findById(event.getRecipientId())
                .map(UserInfoCache::getDisplayName)
                .orElse("User (ID: " + event.getRecipientId() + ")");

        Notification notification = new Notification();
        notification.setRecipientId(event.getSenderId());
        notification.setSenderId(event.getRecipientId()); // The accepter is the sender of this notif
        notification.setContentMessage(
                userName + " accepted your invitation to project: " + event.getProjectName() + " (ID: "
                        + event.getProjectId() + ")");
        notification.setType(NotificationType.INFO_ONLY);
        notification.setReferenceId(event.getProjectId());
        notification.setIsRead(false);
        notification.setInvitationStatus(InvitationStatus.ACCEPTED); // Just for info
        notification.setCreatedAt(LocalDateTime.now());

        sseService.sendToUser(event.getSenderId(), toNotificationDTO(notification));

        notificationRepository.save(notification);

        changeInvitationStatus(event.getToken(), InvitationStatus.ACCEPTED);
    }

    private void createDeclinedNotification(ProjectInvitationEvent event) {
        String userName = userInfoCacheRepository.findById(event.getRecipientId())
                .map(UserInfoCache::getDisplayName)
                .orElse("User (ID: " + event.getRecipientId() + ")");

        Notification notification = new Notification();
        notification.setRecipientId(event.getSenderId());
        notification.setSenderId(event.getRecipientId());
        notification.setContentMessage(
                userName + " declined your invitation to project: " + event.getProjectName() + " (ID: "
                        + event.getProjectId() + ")");
        notification.setType(NotificationType.INFO_ONLY);
        notification.setReferenceId(event.getProjectId());
        notification.setIsRead(false);
        notification.setInvitationStatus(InvitationStatus.DECLINED);
        notification.setCreatedAt(LocalDateTime.now());

        sseService.sendToUser(event.getSenderId(), toNotificationDTO(notification));

        notificationRepository.save(notification);

        changeInvitationStatus(event.getToken(), InvitationStatus.DECLINED);
    }

    private void changeInvitationStatus(String token, InvitationStatus status) {
        Notification notification = notificationRepository.getNotificationByToken(token);
        if (notification == null) {
            throw new RuntimeException("Invitation not found");
        }
        notification.setType(NotificationType.INFO_ONLY);
        notification.setToken(null);
        notification.setIsRead(true);
        notification.setInvitationStatus(status);

        notificationRepository.save(notification);
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

        dto.setInvitationStatus(notification.getInvitationStatus().name());

        dto.setIsRead(notification.getIsRead());
        dto.setToken(notification.getToken());
        dto.setReferenceId(notification.getReferenceId());

        // Format createdAt to ISO 8601 string
        dto.setCreatedAt(notification.getCreatedAt().format(ISO_FORMATTER));

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
                userInfoCache.getAvatarUrl()
            );
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
