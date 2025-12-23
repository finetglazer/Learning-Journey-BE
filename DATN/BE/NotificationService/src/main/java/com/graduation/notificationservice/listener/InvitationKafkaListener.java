package com.graduation.notificationservice.listener;

import com.graduation.notificationservice.config.KafkaConfig;
import com.graduation.notificationservice.event.ProjectInvitationEvent;
import com.graduation.notificationservice.model.Notification;
import com.graduation.notificationservice.model.ProcessedMessage;
import com.graduation.notificationservice.model.UserInfoCache;
import com.graduation.notificationservice.model.enums.InvitationStatus;
import com.graduation.notificationservice.model.enums.NotificationType;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationKafkaListener {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final UserInfoCacheRepository userInfoCacheRepository;
    private final SseService sseService;

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
                /* ignore */ }
            throw e; // Retry
        }
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

        sseService.sendToUser(event.getRecipientId(), notification);

        notificationRepository.save(notification);
    }

    private void createAcceptedNotification(ProjectInvitationEvent event) {
        String userName = userInfoCacheRepository.findById(event.getRecipientId())
                .map(UserInfoCache::getDisplayName)
                .orElse("User (ID: " + event.getRecipientId() + ")");

        Notification notification = new Notification();
        notification.setRecipientId(event.getSenderId());
        notification.setSenderId(event.getRecipientId()); // The accepter is the sender of this notif
        notification.setContentMessage(
                userName + " accepted your invitation to project: " + event.getProjectName() + " (ID: " + event.getProjectId() + ")");
        notification.setType(NotificationType.INFO_ONLY);
        notification.setReferenceId(event.getProjectId());
        notification.setIsRead(false);
        notification.setInvitationStatus(InvitationStatus.ACCEPTED); // Just for info
        notification.setCreatedAt(LocalDateTime.now());

        sseService.sendToUser(event.getRecipientId(), notification);

        notificationRepository.save(notification);
    }

    private void createDeclinedNotification(ProjectInvitationEvent event) {
        String userName = userInfoCacheRepository.findById(event.getRecipientId())
                .map(UserInfoCache::getDisplayName)
                .orElse("User (ID: " + event.getRecipientId() + ")");

        Notification notification = new Notification();
        notification.setRecipientId(event.getSenderId());
        notification.setSenderId(event.getRecipientId());
        notification.setContentMessage(
                userName + " declined your invitation to project: " + event.getProjectName() + " (ID: " + event.getProjectId() + ")");
        notification.setType(NotificationType.INFO_ONLY);
        notification.setReferenceId(event.getProjectId());
        notification.setIsRead(false);
        notification.setInvitationStatus(InvitationStatus.DECLINED);
        notification.setCreatedAt(LocalDateTime.now());

        sseService.sendToUser(event.getRecipientId(), notification);

        notificationRepository.save(notification);
    }

    private String generateMessageId(ConsumerRecord<String, ?> record) {
        return String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
    }
}
