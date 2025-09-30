package com.graduation.sagaorchestratorservice.repository;

import com.graduation.sagaorchestratorservice.model.ProcessedMessage;
import com.graduation.sagaorchestratorservice.model.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ProcessedMessage entities
 */
@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    /**
     * Find processed message by message ID
     */
    Optional<ProcessedMessage> findByMessageId(String messageId);

    /**
     * Find processed messages by saga ID
     */
    List<ProcessedMessage> findBySagaId(String sagaId);

    /**
     * Find processed message by saga ID and step ID
     */
    Optional<ProcessedMessage> findBySagaIdAndStepId(String sagaId, Integer stepId);

    /**
     * Find processed messages by saga ID ordered by step ID
     */
    @Query("SELECT pm FROM ProcessedMessage pm WHERE pm.sagaId = :sagaId ORDER BY pm.stepId ASC")
    List<ProcessedMessage> findBySagaIdOrderByStepIdAsc(@Param("sagaId") String sagaId);

    /**
     * Find processed messages by message type
     */
    List<ProcessedMessage> findByMessageType(String messageType);

    /**
     * Find processed messages by saga ID and message type
     */
    List<ProcessedMessage> findBySagaIdAndMessageType(String sagaId, String messageType);

    /**
     * Check if a message exists by saga ID and step ID
     */
    boolean existsBySagaIdAndStepId(String sagaId, Integer stepId);

    /**
     * Check if any message exists for a saga
     */
    boolean existsBySagaId(String sagaId);

    /**
     * Delete messages older than a certain time
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedMessage pm WHERE pm.processedAt < :cutoffTime")
    int deleteByProcessedAtBefore(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Count processed messages by saga ID
     */
    @Query("SELECT COUNT(pm) FROM ProcessedMessage pm WHERE pm.sagaId = :sagaId")
    long countBySagaId(@Param("sagaId") String sagaId);

    /**
     * Find messages processed within a time range
     */
    @Query("SELECT pm FROM ProcessedMessage pm WHERE pm.processedAt BETWEEN :startTime AND :endTime")
    List<ProcessedMessage> findByProcessedAtBetween(@Param("startTime") Instant startTime,
                                                    @Param("endTime") Instant endTime);

    /**
     * Find the latest processed message for a saga
     */
    @Query("SELECT pm FROM ProcessedMessage pm WHERE pm.sagaId = :sagaId ORDER BY pm.processedAt DESC LIMIT 1")
    Optional<ProcessedMessage> findLatestBySagaId(@Param("sagaId") String sagaId);

    Optional<ProcessedMessage> findBySagaIdAndStepIdAndActionType(String sagaId, Integer stepId, ActionType actionType);
}