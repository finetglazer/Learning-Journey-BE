package com.graduation.sagaorchestratorservice.model.enums;

import lombok.Getter;

/**
 * Enum defining event types for Order Purchase Saga responses
 * Maps to CommandType for graduation project
 */
@Getter
public enum EventType {

    // Payment Service Events
    PAYMENT_PROCESSED("Payment processed successfully"),
    PAYMENT_FAILED("Payment processing failed"),
    PAYMENT_REVERSED("Payment cancelled/refunded"),
    PAYMENT_REVERSED_FAILED("Payment cancellation failed"),

    // Order Service Events
    ORDER_CREATED("Order created successfully"),
    ORDER_STATUS_UPDATED_CONFIRMED("Order status updated to confirmed"),
    ORDER_STATUS_UPDATE_FAILED("Order status update failed"),
    ORDER_STATUS_UPDATED_DELIVERED("Order status updated to delivered"),
    ORDER_CANCELLED("Order cancelled successfully"),
    ORDER_CANCELLATION_FAILED("Order cancellation failed"),

    // Saga Events
    SAGA_COMPLETED("Saga completed successfully"),
    SAGA_FAILED("Saga execution failed");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    /**
     * Get the associated command type for this event
     */
    public CommandType getAssociatedCommandType() {
        return switch (this) {
            case ORDER_CREATED ->
                    CommandType.START_SAGA;
            case PAYMENT_PROCESSED, PAYMENT_FAILED ->
                    CommandType.PAYMENT_PROCESS;
            case PAYMENT_REVERSED, PAYMENT_REVERSED_FAILED ->
                    CommandType.PAYMENT_REVERSE;
            case ORDER_STATUS_UPDATED_CONFIRMED, ORDER_STATUS_UPDATE_FAILED ->
                    CommandType.ORDER_UPDATE_CONFIRMED;
            case ORDER_STATUS_UPDATED_DELIVERED ->
                    CommandType.ORDER_UPDATE_DELIVERED;
            case ORDER_CANCELLED, ORDER_CANCELLATION_FAILED ->
                    CommandType.ORDER_CANCEL;
            case SAGA_COMPLETED, SAGA_FAILED ->
                    CommandType.COMPLETE_SAGA;
        };
    }

    /**
     * Check if this event indicates success
     */
    public boolean isSuccessEvent() {
        return this == PAYMENT_PROCESSED ||
                this == PAYMENT_REVERSED ||
                this == ORDER_STATUS_UPDATED_CONFIRMED ||
                this == ORDER_STATUS_UPDATED_DELIVERED ||
                this == ORDER_CANCELLED ||
                this == SAGA_COMPLETED;
    }

    /**
     * Check if this event indicates failure
     */
    public boolean isFailureEvent() {
        return !isSuccessEvent();
    }
}