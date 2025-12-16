package com.graduation.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity to track processed Kafka messages for idempotency.
 * Prevents duplicate processing of the same message.
 */
@Data
@Entity
@Table(name = "processed_messages")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedMessage {

    /**
     * Unique message identifier (Kafka record key + offset or UUID).
     */
    @Id
    @Column(name = "message_id")
    private String messageId;

    /**
     * Source topic name.
     */
    @Column(name = "topic")
    private String topic;

    /**
     * Timestamp when the message was processed.
     */
    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Processing status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProcessStatus status;

    /**
     * Status enum for message processing.
     */
    public enum ProcessStatus {
        SUCCESS,
        FAILED
    }
}
