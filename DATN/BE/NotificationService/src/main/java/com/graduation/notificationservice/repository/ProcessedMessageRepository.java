package com.graduation.notificationservice.repository;

import com.graduation.notificationservice.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ProcessedMessage entity.
 * Used for idempotency checks to prevent duplicate message processing.
 */
@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    /**
     * Check if a message has already been processed.
     *
     * @param messageId The unique message identifier
     * @return true if the message exists (already processed)
     */
    boolean existsByMessageId(String messageId);
}
