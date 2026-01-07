package com.graduation.schedulingservice.listener;

import com.graduation.schedulingservice.config.BirthdayKafkaConfig;
import com.graduation.schedulingservice.event.BirthdayEvent;
import com.graduation.schedulingservice.service.BirthdayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for birthday update events.
 * Creates/updates birthday memorable events when users update their date of
 * birth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayKafkaListener {

    private final BirthdayService birthdayService;

    /**
     * Handle birthday updated events from Kafka.
     * Creates or updates a "My Birthday" memorable event for the user.
     *
     * @param record The Kafka consumer record
     * @param ack    Acknowledgment for manual commit
     */
    @KafkaListener(topics = BirthdayKafkaConfig.TOPIC_BIRTHDAY_UPDATED, groupId = "${spring.kafka.consumer.group-id:scheduling-service}", containerFactory = "birthdayKafkaListenerContainerFactory")
    public void handleBirthdayUpdatedEvent(ConsumerRecord<String, BirthdayEvent> record, Acknowledgment ack) {
        BirthdayEvent event = record.value();
        String messageId = generateMessageId(record);

        log.info("Received BirthdayEvent for userId={}, day={}, month={}, messageId={}",
                event.getUserId(), event.getDay(), event.getMonth(), messageId);

        try {
            // Create or update the birthday memorable event
            birthdayService.createOrUpdateBirthday(event.getUserId(), event.getDay(), event.getMonth());

            log.info("Successfully processed BirthdayEvent for userId={}", event.getUserId());

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process BirthdayEvent for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);

            // Re-throw to trigger retry/DLQ
            throw e;
        }
    }

    /**
     * Generate a unique message ID from the Kafka record.
     */
    private String generateMessageId(ConsumerRecord<String, ?> record) {
        return String.format("%s-%d-%d",
                record.topic(),
                record.partition(),
                record.offset());
    }
}
