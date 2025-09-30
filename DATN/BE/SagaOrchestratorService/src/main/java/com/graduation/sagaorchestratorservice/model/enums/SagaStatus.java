package com.graduation.sagaorchestratorservice.model.enums;

import lombok.Getter;

/**
 * Enum representing the various states of a saga during its lifecycle
 */
@Getter
public enum SagaStatus {

    /**
     * Saga has been initiated and is starting
     */
    STARTED("Saga has been started"),

    /**
     * Saga is currently executing steps
     */
    IN_PROGRESS("Saga is in progress"),

    /**
     * Saga completed successfully
     */
    COMPLETED("Saga completed successfully"),

    /**
     * Saga failed and needs compensation
     */
    FAILED("Saga execution failed"),

    /**
     * Saga is executing compensation steps
     */
    COMPENSATING("Saga is compensating"),

    /**
     * Compensation process completed
     */
    COMPENSATION_COMPLETED("Compensation completed"),

    /**
     * Compensation failed - manual intervention needed
     */
    COMPENSATION_FAILED("Compensation failed - manual intervention required");

    private final String description;

    SagaStatus(String description) {
        this.description = description;
    }

    /**
     * Check if saga is in an active state (not final)
     */
    public boolean isActive() {
        return this == STARTED || this == IN_PROGRESS || this == COMPENSATING;
    }

    /**
     * Check if saga is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == COMPENSATION_COMPLETED || this == COMPENSATION_FAILED;
    }

    /**
     * Check if saga is successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Check if saga failed
     */
    public boolean isFailed() {
        return this == FAILED || this == COMPENSATION_COMPLETED || this == COMPENSATION_FAILED;
    }

    /**
     * Get next possible statuses from current status
     */
    public SagaStatus[] getPossibleNextStatuses() {
        return switch (this) {
            case STARTED -> new SagaStatus[]{IN_PROGRESS, FAILED};
            case IN_PROGRESS -> new SagaStatus[]{COMPLETED, FAILED};
            case FAILED -> new SagaStatus[]{COMPENSATING};
            case COMPENSATING -> new SagaStatus[]{COMPENSATION_COMPLETED};
            case COMPLETED, COMPENSATION_COMPLETED, COMPENSATION_FAILED -> new SagaStatus[]{}; // Final states
        };
    }
}