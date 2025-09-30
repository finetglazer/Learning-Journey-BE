package com.graduation.sagaorchestratorservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity to track processed messages for idempotency
 */
@Slf4j
@Entity
@Table(name = Constant.TABLE_PROCESSED_MESSAGES, indexes = {
        @Index(name = Constant.INDEX_PROCESSED_MESSAGE_SAGA_STEP, columnList = Constant.COLUMN_SAGA_ID + ", " + Constant.COLUMN_STEP_ID),
        @Index(name = Constant.INDEX_PROCESSED_MESSAGE_PROCESSED_AT, columnList = Constant.COLUMN_PROCESSED_AT)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedMessage {

    @Id
    @Column(name = Constant.COLUMN_MESSAGE_ID, nullable = false)
    private String messageId;

    @Column(name = Constant.COLUMN_SAGA_ID)
    private String sagaId;

    @Column(name = Constant.COLUMN_STEP_ID)
    private Integer stepId;

    @Column(name = Constant.COLUMN_MESSAGE_TYPE, nullable = false)
    private String messageType;

    @Column(name = Constant.COLUMN_RESULT_JSON, columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = Constant.COLUMN_PROCESSED_AT, nullable = false)
    private Instant processedAt;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_ACTION_TYPE)
    private ActionType actionType;

    /**
     * Factory method to create a ProcessedMessage
     */
    public static ProcessedMessage create(String messageId, String sagaId, Integer stepId,
                                          String messageType, Map<String, Object> result) {
        return ProcessedMessage.builder()
                .messageId(messageId)
                .sagaId(sagaId)
                .stepId(stepId)
                .messageType(messageType)
                .resultJson(mapToJson(result))
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Get the result as a Map
     */
    public Map<String, Object> getResult() {
        if (resultJson == null || resultJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error(Constant.ERROR_PARSE_RESULT_JSON, messageId, resultJson, e);
            return new HashMap<>();
        }
    }

    /**
     * Set the result from a Map
     */
    public void setResult(Map<String, Object> result) {
        this.resultJson = mapToJson(result);
    }

    /**
     * Helper method to convert Map to JSON string
     */
    private static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Constant.DEFAULT_EMPTY_JSON;
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error(Constant.ERROR_SERIALIZE_RESULT_MAP, map, e);
            return Constant.DEFAULT_EMPTY_JSON;
        }
    }

    /**
     * Pre-persist callback to ensure processedAt is set
     */
    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_PROCESSED_MESSAGE_TOSTRING,
                messageId, sagaId, stepId, messageType, processedAt);
    }
}