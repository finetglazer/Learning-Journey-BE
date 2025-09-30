package com.graduation.sagaorchestratorservice.model;

import com.graduation.sagaorchestratorservice.constants.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an event in the saga execution history
 * Used for tracking and auditing saga progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {

    private String type;
    private String description;
    private Instant timestamp;

    /**
     * Factory method to create a saga event
     */
    public static SagaEvent of(String type, String description) {
        return SagaEvent.builder()
                .type(type)
                .description(description)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Factory method to create a step start event
     */
    public static SagaEvent stepStarted(String stepName) {
        return of(Constant.SAGA_EVENT_STEP_STARTED, Constant.DESC_STEP_STARTED + stepName);
    }

    /**
     * Factory method to create a step completed event
     */
    public static SagaEvent stepCompleted(String stepName) {
        return of(Constant.SAGA_EVENT_STEP_COMPLETED, Constant.DESC_STEP_COMPLETED + stepName);
    }

    /**
     * Factory method to create a step failed event
     */
    public static SagaEvent stepFailed(String stepName, String reason) {
        return of(Constant.SAGA_EVENT_STEP_FAILED, String.format(Constant.DESC_STEP_FAILED, stepName, reason));
    }

    /**
     * Factory method to create a compensation started event
     */
    public static SagaEvent compensationStarted(String reason) {
        return of(Constant.SAGA_EVENT_COMPENSATION_STARTED, Constant.DESC_COMPENSATION_STARTED + reason);
    }

    /**
     * Factory method to create a compensation completed event
     */
    public static SagaEvent compensationCompleted() {
        return of(Constant.SAGA_EVENT_COMPENSATION_COMPLETED, Constant.DESC_COMPENSATION_COMPLETED);
    }

    /**
     * Factory method to create a saga completed event
     */
    public static SagaEvent sagaCompleted() {
        return of(Constant.SAGA_EVENT_SAGA_COMPLETED, Constant.DESC_SAGA_COMPLETED);
    }

    /**
     * Factory method to create a saga failed event
     */
    public static SagaEvent sagaFailed(String reason) {
        return of(Constant.SAGA_EVENT_SAGA_FAILED, Constant.DESC_SAGA_FAILED + reason);
    }

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_SAGA_EVENT_TOSTRING, timestamp, type, description);
    }
}