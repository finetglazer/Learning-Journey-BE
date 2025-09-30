package com.graduation.sagaorchestratorservice.exception;

import lombok.Getter;

/**
 * Exception thrown when saga execution encounters an error
 */
@Getter
public class SagaExecutionException extends RuntimeException {

    private final String sagaId;
    private final Integer stepId;

    public SagaExecutionException(String message) {
        super(message);
        this.sagaId = null;
        this.stepId = null;
    }

    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.sagaId = null;
        this.stepId = null;
    }

    public SagaExecutionException(String sagaId, String message) {
        super(String.format("Saga [%s]: %s", sagaId, message));
        this.sagaId = sagaId;
        this.stepId = null;
    }

    public SagaExecutionException(String sagaId, String message, Throwable cause) {
        super(String.format("Saga [%s]: %s", sagaId, message), cause);
        this.sagaId = sagaId;
        this.stepId = null;
    }

    public SagaExecutionException(String sagaId, Integer stepId, String message) {
        super(String.format("Saga [%s] Step [%d]: %s", sagaId, stepId, message));
        this.sagaId = sagaId;
        this.stepId = stepId;
    }

    public SagaExecutionException(String sagaId, Integer stepId, String message, Throwable cause) {
        super(String.format("Saga [%s] Step [%d]: %s", sagaId, stepId, message), cause);
        this.sagaId = sagaId;
        this.stepId = stepId;
    }

}