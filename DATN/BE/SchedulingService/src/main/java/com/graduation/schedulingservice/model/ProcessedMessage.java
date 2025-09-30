package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing processed messages for idempotency
 */
@Data
@Entity
@Table(name = Constant.TABLE_PROCESSED_MESSAGES)
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedMessage {

    @Id
    @Column(name = Constant.COLUMN_MESSAGE_ID)
    private String messageId;

    @Column(name = Constant.COLUMN_SAGA_ID)
    private String sagaId;

    @Column(name = Constant.COLUMN_PROCESSED_AT)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_STATUS)
    private ProcessStatus status;

    // Status Enum
    public enum ProcessStatus {
        SUCCESS, FAILED, COMPENSATED
    }

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_PROCESSED_MESSAGE_TOSTRING,
                messageId, sagaId, status, processedAt);
    }
}