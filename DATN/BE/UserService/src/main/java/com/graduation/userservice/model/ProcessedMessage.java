// ProcessedMessage.java - Model/Entity
package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}