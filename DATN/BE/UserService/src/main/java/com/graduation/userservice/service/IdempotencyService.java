package com.graduation.userservice.service;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.model.ProcessedMessage;
import com.graduation.userservice.repository.ProcessedMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final ProcessedMessageRepository processedMessageRepository;

    /**
     * Check if a message has been processed before
     * This handles different scenarios:
     * 1. Message with messageId
     * 2. Message identified by sagaId combination
     */
    public boolean isProcessed(String messageId, String sagaId) {
        // Validate messageId is provided (sagaId is only for context/logging)
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException(Constant.ERROR_MESSAGE_ID_REQUIRED);
        }

        // Check by messageId only (each command should have unique messageId)
        if (processedMessageRepository.findByMessageId(messageId).isPresent()) {
            log.debug(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
            return true;
        }

        log.info(Constant.LOG_MESSAGE_NOT_PROCESSED, messageId, sagaId);
        return false;
    }

    public void recordProcessing(String messageId, String sagaId, ProcessedMessage.ProcessStatus status) {
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException(Constant.ERROR_MESSAGE_ID_REQUIRED_RECORD);
        }
        if (sagaId == null || sagaId.isEmpty()) {
            throw new IllegalArgumentException(Constant.ERROR_SAGA_ID_REQUIRED);
        }
        try {
            ProcessedMessage processedMessage = new ProcessedMessage(messageId, sagaId, Instant.now(), status);
            processedMessageRepository.save(processedMessage);
            log.info(Constant.LOG_RECORDED_PROCESSING, messageId, sagaId, status);

        } catch (Exception ex) {
            log.error(Constant.LOG_FAILED_TO_RECORD_PROCESSING, messageId, sagaId, ex);
            throw new RuntimeException(Constant.ERROR_FAILED_TO_RECORD, ex);
        }
    }

    /**
     * Clean up old processed messages
     * This can be scheduled to run periodically to remove old entries
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldProcessedMessages() {
        log.info(Constant.LOG_CLEANING_OLD_MESSAGES);

        try {
            Instant cutoffTime = Instant.now().minus(30, ChronoUnit.HOURS);

            int deletedCount = processedMessageRepository.deleteByProcessedAtBefore(cutoffTime);

            log.info(Constant.LOG_DELETED_OLD_MESSAGES, deletedCount);
            if (deletedCount > 0) {
                log.info(Constant.LOG_OLD_MESSAGES_CLEANUP_SUCCESS);
            } else {
                log.info(Constant.LOG_NO_OLD_MESSAGES);
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}