package com.graduation.sagaorchestratorservice.exception;

import lombok.Getter;

/**
 * Exception thrown when a saga cannot be found in the system
 */
@Getter
public class SagaNotFoundException extends RuntimeException {

    private final String sagaId;

    public SagaNotFoundException(String sagaId) {
        super(String.format("Saga with ID [%s] not found", sagaId));
        this.sagaId = sagaId;
    }

    public SagaNotFoundException(String sagaId, String message) {
        super(String.format("Saga [%s]: %s", sagaId, message));
        this.sagaId = sagaId;
    }

    public SagaNotFoundException(String sagaId, String message, Throwable cause) {
        super(String.format("Saga [%s]: %s", sagaId, message), cause);
        this.sagaId = sagaId;
    }

}