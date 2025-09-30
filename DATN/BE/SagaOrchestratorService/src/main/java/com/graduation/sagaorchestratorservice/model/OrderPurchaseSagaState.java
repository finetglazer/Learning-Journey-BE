package com.graduation.sagaorchestratorservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.model.enums.OrderPurchaseSagaStep;
import com.graduation.sagaorchestratorservice.model.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the state of an Order Purchase Saga
 * Tracks the complete lifecycle from order creation to completion
 */
@Slf4j
@Entity
@Table(name = Constant.TABLE_ORDER_PURCHASE_SAGAS, indexes = {
        @Index(name = Constant.INDEX_SAGA_ORDER_ID, columnList = Constant.COLUMN_ORDER_ID),
        @Index(name = Constant.INDEX_SAGA_USER_ID, columnList = Constant.COLUMN_USER_ID),
        @Index(name = Constant.INDEX_SAGA_STATUS, columnList = Constant.COLUMN_STATUS),
        @Index(name = Constant.INDEX_SAGA_START_TIME, columnList = Constant.COLUMN_START_TIME)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPurchaseSagaState {

    @Id
    @Column(name = Constant.COLUMN_SAGA_ID, nullable = false)
    private String sagaId;

    // Business data from order
    @Column(name = Constant.COLUMN_USER_ID, nullable = false)
    private String userId;

    @Column(name = Constant.COLUMN_ORDER_ID, nullable = false)
    private Long orderId;

    @Column(name = Constant.COLUMN_USER_EMAIL)
    private String userEmail;

    @Column(name = Constant.COLUMN_USER_NAME)
    private String userName;

    @Column(name = Constant.COLUMN_ORDER_DESCRIPTION, length = 1000)
    private String orderDescription;

    @Column(name = Constant.COLUMN_TOTAL_AMOUNT, nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Payment processing data
    @Column(name = Constant.COLUMN_PAYMENT_TRANSACTION_ID)
    private Long paymentTransactionId;

    // Saga execution state
    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_CURRENT_STEP)
    private OrderPurchaseSagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_STATUS, nullable = false)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = Constant.COLUMN_COMPLETED_STEPS, columnDefinition = "TEXT")
    private String completedStepsJson;

    @Column(name = Constant.COLUMN_SAGA_EVENTS, columnDefinition = "TEXT")
    private String sagaEventsJson;

    @Column(name = Constant.COLUMN_FAILURE_REASON, length = 1000)
    private String failureReason;

    // Timing information
    @Column(name = Constant.COLUMN_START_TIME, nullable = false)
    private Instant startTime;

    @Column(name = Constant.COLUMN_END_TIME)
    private Instant endTime;

    @Column(name = Constant.COLUMN_LAST_UPDATED_TIME, nullable = false)
    private Instant lastUpdatedTime;

    @Column(name = Constant.COLUMN_CURRENT_STEP_START_TIME)
    private Instant currentStepStartTime;

    // Retry information
    @Column(name = Constant.COLUMN_RETRY_COUNT)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = Constant.COLUMN_MAX_RETRIES)
    @Builder.Default
    private Integer maxRetries = Constant.DEFAULT_MAX_RETRIES;

    @Getter
    @Column(name = Constant.COLUMN_COMPENSATION_RETRY_COUNT)
    private int compensationRetryCount = 0;

    @Column(name = Constant.COLUMN_MAX_COMPENSATION_RETRIES)
    private int maxCompensationRetries = Constant.DEFAULT_MAX_COMPENSATION_RETRIES;

    public void incrementCompensationRetryCount() {
        this.compensationRetryCount++;
        this.lastUpdatedTime = Instant.now();
    }

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    private List<String> completedSteps;

    @Transient
    private List<SagaEvent> sagaEvents;

    /**
     * Factory method to initiate a new order purchase saga
     */
    public static OrderPurchaseSagaState initiate(String sagaId, String userId, Long orderId,
                                                  String userEmail, String userName,
                                                  String orderDescription, BigDecimal totalAmount) {
        OrderPurchaseSagaState saga = OrderPurchaseSagaState.builder()
                .sagaId(sagaId)
                .userId(userId)
                .orderId(orderId)
                .userEmail(userEmail)
                .userName(userName)
                .orderDescription(orderDescription)
                .totalAmount(totalAmount)
                .currentStep(OrderPurchaseSagaStep.PROCESS_PAYMENT)
                .status(SagaStatus.STARTED)
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(Constant.DEFAULT_MAX_RETRIES)
                .build();

        saga.initializeCollections();
        saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_INITIATED,
                Constant.DESC_SAGA_INITIATED + orderId));

        return saga;
    }

    /**
     * Move to the next step in the saga
     */
    public void moveToNextStep() {
        // Add current step to completed steps
        if (currentStep != null && !currentStep.isCompensationStep()) {
            log.info("Adding completed step: {} for saga: {}", currentStep.name(), sagaId);
            getCompletedSteps().add(currentStep.name());
        }

        // Get next step
        OrderPurchaseSagaStep nextStep = currentStep.getNextStep();
        currentStep = nextStep;
        currentStepStartTime = Instant.now();

        if (nextStep == OrderPurchaseSagaStep.COMPLETE_SAGA) {
            log.info("Saga [{}] completed successfully", sagaId);
            status = SagaStatus.COMPLETED;
            endTime = Instant.now();
            addEvent(SagaEvent.sagaCompleted());
        } else {
            status = SagaStatus.IN_PROGRESS;
            addEvent(SagaEvent.stepStarted(nextStep.getDescription()));
        }

        lastUpdatedTime = Instant.now();
    }

    /**
     * Handle a step failure
     */
    public void handleFailure(String reason, String stepName) {
        failureReason = reason;
        status = SagaStatus.FAILED;
        addEvent(SagaEvent.stepFailed(stepName, reason));
        lastUpdatedTime = Instant.now();
    }

    /**
     * Start compensation process
     */
    public void startCompensation() {
        status = SagaStatus.COMPENSATING;
        addEvent(SagaEvent.compensationStarted(failureReason));

        // Determine first compensation step based on completed steps
        boolean paymentProcessed = getCompletedSteps().contains(OrderPurchaseSagaStep.PROCESS_PAYMENT.name());
        boolean orderStatusConfirmed = getCompletedSteps().contains(OrderPurchaseSagaStep.UPDATE_ORDER_STATUS_CONFIRMED.name());
        boolean orderStatusDelivered = getCompletedSteps().contains(OrderPurchaseSagaStep.UPDATE_ORDER_STATUS_DELIVERED.name());

        currentStep = OrderPurchaseSagaStep.determineFirstCompensationStep(
                paymentProcessed, orderStatusConfirmed, orderStatusDelivered);

        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_STEP,
                Constant.DESC_COMPENSATION_STEP + currentStep.getDescription()));
    }

    /**
     * Complete compensation process
     */
    public void completeCompensation() {
        log.info("✅ Compensation completed for saga [{}]", sagaId);
        status = SagaStatus.COMPENSATION_COMPLETED;
        currentStep = OrderPurchaseSagaStep.COMPLETE_SAGA;
        endTime = Instant.now();
        addEvent(SagaEvent.compensationCompleted());
        lastUpdatedTime = Instant.now();
    }

    /**
     * Add an event to the saga history
     */
    public void addEvent(SagaEvent event) {
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
        sagaEvents.add(event);
    }

    /**
     * Check if the current step has timed out
     */
    public boolean isCurrentStepTimedOut(Duration timeout) {
        if (currentStepStartTime == null) {
            return false;
        }
        return Duration.between(currentStepStartTime, Instant.now()).compareTo(timeout) > 0;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        retryCount++;
        lastUpdatedTime = Instant.now();
    }

    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= maxRetries;
    }

    /**
     * Get completed steps list
     */
    public List<String> getCompletedSteps() {
        if (completedSteps == null) {
            if (completedStepsJson != null && !completedStepsJson.trim().isEmpty()) {
                try {
                    completedSteps = objectMapper.readValue(completedStepsJson, new TypeReference<List<String>>() {});
                } catch (JsonProcessingException e) {
                    log.error(Constant.ERROR_PARSE_COMPLETED_STEPS, sagaId, completedStepsJson, e);
                    completedSteps = new ArrayList<>();
                }
            } else {
                completedSteps = new ArrayList<>();
            }
        }
        return completedSteps;
    }

    /**
     * Get saga events list
     */
    public List<SagaEvent> getSagaEvents() {
        if (sagaEvents == null) {
            if (sagaEventsJson != null && !sagaEventsJson.trim().isEmpty()) {
                try {
                    sagaEvents = objectMapper.readValue(sagaEventsJson, new TypeReference<List<SagaEvent>>() {});
                } catch (JsonProcessingException e) {
                    log.error(Constant.ERROR_PARSE_SAGA_EVENTS, sagaId, sagaEventsJson, e);
                    sagaEvents = new ArrayList<>();
                }
            } else {
                sagaEvents = new ArrayList<>();
            }
        }
        return sagaEvents;
    }

    /**
     * Initialize collections if null
     */
    private void initializeCollections() {
        if (completedSteps == null) {
            completedSteps = new ArrayList<>();
        }
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
    }

    /**
     * Pre-persist callback to serialize collections
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Configure ObjectMapper once, properly
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if (completedSteps != null) {
            try {
                completedStepsJson = objectMapper.writeValueAsString(completedSteps);
            } catch (JsonProcessingException e) {
                log.error(Constant.ERROR_SERIALIZE_COMPLETED_STEPS, sagaId, e);
                completedStepsJson = Constant.DEFAULT_EMPTY_ARRAY;
            }
        }

        if (sagaEvents != null) {
            try {
                sagaEventsJson = objectMapper.writeValueAsString(sagaEvents);
            } catch (JsonProcessingException e) {
                log.error(Constant.ERROR_SERIALIZE_SAGA_EVENTS, sagaId, e);
                sagaEventsJson = Constant.DEFAULT_EMPTY_ARRAY;
            }
        }

        if (lastUpdatedTime == null) {
            lastUpdatedTime = Instant.now();
        }
    }

    /**
     * Post-load callback to deserialize collections
     */
    @PostLoad
    public void postLoad() {
        // Initialize transient collections from JSON
        getCompletedSteps();
        getSagaEvents();
    }

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_SAGA_TOSTRING,
                sagaId, orderId, userId, status, currentStep);
    }
}