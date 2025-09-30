package com.graduation.sagaorchestratorservice.service;

import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.model.ProcessedMessage;
import com.graduation.sagaorchestratorservice.model.enums.ActionType;
import com.graduation.sagaorchestratorservice.repository.ProcessedMessageRepository;
import com.graduation.sagaorchestratorservice.utils.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for ensuring idempotent message processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedMessageRepository processedMessageRepository;

    /**
     * Check if a message has been processed before
     * This handles different scenarios:
     * 1. Message with messageId
     * 2. Message identified by sagaId + stepId combination
     */
    public boolean isProcessed(String messageId, String sagaId, Integer stepId, String messageType, ActionType actionType) {
        // Primary check: by messageId if available
        if (messageId != null && !messageId.trim().isEmpty()) {
            Optional<ProcessedMessage> processed = processedMessageRepository.findByMessageId(messageId);
            if (processed.isPresent()) {
                log.debug(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId);
                return true;
            }
        } else {
            throw new IllegalArgumentException(Constant.VALIDATION_MESSAGE_ID_REQUIRED);
        }

        // Secondary check: by sagaId + stepId + actionType combination
        if (sagaId != null && stepId != null && actionType != null) {
            Optional<ProcessedMessage> processed = processedMessageRepository
                    .findBySagaIdAndStepIdAndActionType(sagaId, stepId, actionType);
            if (processed.isPresent()) {
                log.debug(Constant.LOG_MESSAGE_ALREADY_PROCESSED_SAGA_STEP,
                        sagaId, stepId, actionType);
                return true;
            }
        } else {
            throw new IllegalArgumentException(Constant.VALIDATION_SAGA_ID_REQUIRED + " and " + Constant.VALIDATION_STEP_ID_REQUIRED);
        }

        log.debug(Constant.LOG_MESSAGE_NOT_PROCESSED,
                messageId, sagaId, stepId, actionType);
        return false;
    }

    /**
     * Record that a message has been processed
     */
    @Transactional
    public void recordProcessing(String messageId, String sagaId, Integer stepId,
                                 String messageType, Map<String, Object> result) {

        // Generate messageId if not provided
        String finalMessageId = messageId;
        if (finalMessageId == null || finalMessageId.trim().isEmpty()) {
            if (sagaId != null && stepId != null) {
                finalMessageId = MessageIdGenerator.generateForSagaStep(sagaId, stepId);
                log.debug(Constant.LOG_GENERATED_MESSAGE_ID,
                        finalMessageId, sagaId, stepId);
            } else {
                finalMessageId = MessageIdGenerator.generate();
                log.warn(Constant.LOG_GENERATED_RANDOM_MESSAGE_ID,
                        finalMessageId);
            }
        }

        try {
            ProcessedMessage processedMessage = ProcessedMessage.create(
                    finalMessageId,
                    sagaId,
                    stepId,
                    messageType,
                    result
            );

            processedMessageRepository.save(processedMessage);
            log.debug(Constant.LOG_RECORDED_MESSAGE_PROCESSING,
                    finalMessageId, sagaId, stepId);

        } catch (Exception e) {
            log.error(Constant.LOG_FAILED_RECORD_MESSAGE_PROCESSING,
                    finalMessageId, sagaId, stepId, e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    /**
     * Get previously processed result for a message
     */
    public Optional<Map<String, Object>> getProcessedResult(String messageId, String sagaId, Integer stepId) {
        ProcessedMessage processedMessage = null;

        // Try to find by messageId first
        if (messageId != null && !messageId.trim().isEmpty()) {
            processedMessage = processedMessageRepository.findByMessageId(messageId).orElse(null);
        }

        // Fallback to sagaId + stepId
        if (processedMessage == null && sagaId != null && stepId != null) {
            processedMessage = processedMessageRepository.findBySagaIdAndStepId(sagaId, stepId).orElse(null);
        }

        if (processedMessage != null) {
            log.debug(Constant.LOG_FOUND_PROCESSED_RESULT,
                    messageId, sagaId, stepId);
            return Optional.of(processedMessage.getResult());
        }

        log.debug(Constant.LOG_NO_PROCESSED_RESULT,
                messageId, sagaId, stepId);
        return Optional.empty();
    }

    /**
     * Get previously processed result for a specific saga step
     */
    public Optional<Map<String, Object>> getProcessedResultForStep(String sagaId, Integer stepId) {
        if (sagaId == null || stepId == null) {
            return Optional.empty();
        }

        Optional<ProcessedMessage> processedMessage =
                processedMessageRepository.findBySagaIdAndStepId(sagaId, stepId);

        return processedMessage.map(ProcessedMessage::getResult);
    }

    /**
     * Check if a specific saga step has been processed
     */
    public boolean isStepProcessed(String sagaId, Integer stepId) {
        if (sagaId == null || stepId == null) {
            return false;
        }

        return processedMessageRepository.existsBySagaIdAndStepId(sagaId, stepId);
    }

    /**
     * Get all processed messages for a saga
     */
    public java.util.List<ProcessedMessage> getProcessedMessagesForSaga(String sagaId) {
        if (sagaId == null) {
            return java.util.List.of();
        }

        return processedMessageRepository.findBySagaIdOrderByStepIdAsc(sagaId);
    }

    /**
     * Get the count of processed messages for a saga
     */
    public long getProcessedMessageCount(String sagaId) {
        if (sagaId == null) {
            return 0;
        }

        return processedMessageRepository.countBySagaId(sagaId);
    }

    /**
     * Delete processed messages for a specific saga
     * Useful for saga cleanup after completion
     */
    @Transactional
    public void cleanupSagaMessages(String sagaId) {
        if (sagaId == null) {
            return;
        }

        try {
            java.util.List<ProcessedMessage> messages = processedMessageRepository.findBySagaId(sagaId);
            if (!messages.isEmpty()) {
                processedMessageRepository.deleteAll(messages);
                log.info(Constant.LOG_CLEANED_SAGA_MESSAGES, messages.size(), sagaId);
            }
        } catch (Exception e) {
            log.error(Constant.LOG_FAILED_CLEANUP_SAGA_MESSAGES, sagaId, e);
        }
    }

    /**
     * Scheduled task to clean up old processed messages
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldMessages() {
        log.info(Constant.LOG_STARTING_CLEANUP_OLD_MESSAGES);

        try {
            // Keep messages for 30 days (configurable)
            Instant cutoffTime = Instant.now().minus(30, ChronoUnit.DAYS);

            int deletedCount = processedMessageRepository.deleteByProcessedAtBefore(cutoffTime);

            if (deletedCount > 0) {
                log.info(Constant.LOG_COMPLETED_CLEANUP, deletedCount);
            } else {
                log.debug(Constant.LOG_NO_OLD_MESSAGES_CLEANUP);
            }

        } catch (Exception e) {
            log.error(Constant.LOG_FAILED_CLEANUP_OLD_MESSAGES, e);
        }
    }

    /**
     * Manual cleanup method for testing or administrative purposes
     */
    @Transactional
    public int cleanupMessagesOlderThan(int days) {
        try {
            Instant cutoffTime = Instant.now().minus(days, ChronoUnit.DAYS);
            int deletedCount = processedMessageRepository.deleteByProcessedAtBefore(cutoffTime);

            log.info(Constant.LOG_MANUAL_CLEANUP,
                    deletedCount, days);
            return deletedCount;

        } catch (Exception e) {
            log.error(Constant.LOG_FAILED_MANUAL_CLEANUP, e);
            return 0;
        }
    }
}