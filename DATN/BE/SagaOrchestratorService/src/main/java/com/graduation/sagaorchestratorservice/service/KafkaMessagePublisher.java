package com.graduation.sagaorchestratorservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.utils.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing messages to Kafka topics
 * This is a generic publisher that can handle any message type
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a message to a topic with automatic key generation
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(Object message, String topic) {
        String messageId = MessageIdGenerator.generate();
        return publishMessage(message, topic, messageId);
    }

    /**
     * Publish a message to a topic with a specific key
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(Object message, String topic, String key) {
        if (message == null) {
            throw new IllegalArgumentException(Constant.VALIDATION_MESSAGE_NULL);
        }

        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException(Constant.VALIDATION_TOPIC_NULL);
        }

        log.debug(Constant.LOG_PUBLISHING_MESSAGE,
                topic, key, message.getClass().getSimpleName());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

        // Add callback for logging
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(Constant.LOG_FAILED_SEND_MESSAGE,
                        topic, key, throwable.getMessage(), throwable);
            } else {
                log.debug(Constant.LOG_SUCCESS_SEND_MESSAGE,
                        topic, key,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            }
        });

        return future;
    }

    /**
     * Publish a command message with saga context
     */
    public CompletableFuture<SendResult<String, Object>> publishCommand(Map<String, Object> command,
                                                                        String topic, String sagaId) {
        // Ensure command has required fields
        if (!command.containsKey(Constant.FIELD_MESSAGE_ID)) {
            command.put(Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateCommandId());
        }
        if (!command.containsKey(Constant.FIELD_SAGA_ID)) {
            command.put(Constant.FIELD_SAGA_ID, sagaId);
        }
        if (!command.containsKey(Constant.FIELD_TIMESTAMP)) {
            command.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
        }
        if (!command.containsKey(Constant.FIELD_TYPE)) {
            command.put(Constant.FIELD_TYPE, Constant.MESSAGE_TYPE_COMMAND);
        }

        // Use sagaId as partition key to ensure ordering
        return publishMessage(command, topic, sagaId);
    }

    /**
     * Publish an event message with saga context
     */
    public CompletableFuture<SendResult<String, Object>> publishEvent(Map<String, Object> event,
                                                                      String topic, String sagaId) {
        // Ensure event has required fields
        if (!event.containsKey(Constant.FIELD_MESSAGE_ID)) {
            event.put(Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateEventId());
        }
        if (!event.containsKey(Constant.FIELD_SAGA_ID)) {
            event.put(Constant.FIELD_SAGA_ID, sagaId);
        }
        if (!event.containsKey(Constant.FIELD_TIMESTAMP)) {
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
        }
        if (!event.containsKey(Constant.FIELD_TYPE)) {
            event.put(Constant.FIELD_TYPE, Constant.MESSAGE_TYPE_EVENT);
        }

        // Use sagaId as partition key to ensure ordering
        return publishMessage(event, topic, sagaId);
    }

    /**
     * Publish a saga step command
     */
    public CompletableFuture<SendResult<String, Object>> publishSagaStepCommand(String sagaId,
                                                                                Integer stepId,
                                                                                String commandType,
                                                                                Map<String, Object> payload,
                                                                                String topic) {
        Map<String, Object> command = Map.of(
                Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateForSagaStep(sagaId, stepId),
                Constant.FIELD_SAGA_ID, sagaId,
                Constant.FIELD_STEP_ID, stepId,
                Constant.FIELD_TYPE, commandType,
                Constant.FIELD_TIMESTAMP, System.currentTimeMillis(),
                Constant.FIELD_PAYLOAD, payload != null ? payload : Map.of()
        );

        return publishMessage(command, topic, sagaId);
    }

    /**
     * Publish a saga step event
     */
    public CompletableFuture<SendResult<String, Object>> publishSagaStepEvent(String sagaId,
                                                                              Integer stepId,
                                                                              String eventType,
                                                                              Map<String, Object> payload,
                                                                              String topic) {
        Map<String, Object> event = Map.of(
                Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateForSagaStep(sagaId, stepId),
                Constant.FIELD_SAGA_ID, sagaId,
                Constant.FIELD_STEP_ID, stepId,
                Constant.FIELD_TYPE, eventType,
                Constant.FIELD_TIMESTAMP, System.currentTimeMillis(),
                Constant.FIELD_PAYLOAD, payload != null ? payload : Map.of()
        );

        return publishMessage(event, topic, sagaId);
    }

    /**
     * Publish multiple messages in batch (fire and forget)
     */
    public void publishBatch(java.util.List<Map<String, Object>> messages, String topic) {
        if (messages == null || messages.isEmpty()) {
            log.warn(Constant.LOG_NO_MESSAGES_BATCH);
            return;
        }

        log.info(Constant.LOG_PUBLISHING_BATCH, messages.size(), topic);

        for (Map<String, Object> message : messages) {
            try {
                String sagaId = (String) message.get(Constant.FIELD_SAGA_ID);
                publishMessage(message, topic, sagaId != null ? sagaId : MessageIdGenerator.generate());
            } catch (Exception e) {
                log.error(Constant.LOG_FAILED_PUBLISH_BATCH, message, e);
            }
        }
    }

    /**
     * Publish a message to Dead Letter Queue
     */
    public CompletableFuture<SendResult<String, Object>> publishToDLQ(Object originalMessage,
                                                                      String originalTopic,
                                                                      String errorReason) {
        Map<String, Object> dlqMessage = Map.of(
                Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generate(),
                "originalTopic", originalTopic,
                "errorReason", errorReason,
                Constant.FIELD_TIMESTAMP, System.currentTimeMillis(),
                "originalMessage", originalMessage
        );

        // Use a default DLQ topic name
        String dlqTopic = originalTopic + Constant.DLQ_TOPIC_SUFFIX;

        log.warn(Constant.LOG_PUBLISHING_TO_DLQ, dlqTopic, errorReason);

        return publishMessage(dlqMessage, dlqTopic, "DLQ");
    }

    /**
     * Health check method to verify Kafka connectivity
     */
    public boolean isHealthy() {
        try {
            // Simple health check by getting metadata
            kafkaTemplate.getDefaultTopic(); // This will trigger connection check
            return true;
        } catch (Exception e) {
            log.error(Constant.LOG_KAFKA_HEALTH_CHECK_FAILED, e);
            return false;
        }
    }
}