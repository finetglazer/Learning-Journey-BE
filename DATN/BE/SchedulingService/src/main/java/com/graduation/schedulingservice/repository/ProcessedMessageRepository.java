package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.ProcessedMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    Optional<ProcessedMessage> findByMessageId(String messageId);

    Optional<ProcessedMessage> findBySagaId(String sagaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedMessage pm WHERE pm.processedAt < :cutoffTime")
    int deleteByProcessedAtBefore(@Param("cutoffTime") Instant cutoffTime);
}
