package com.graduation.sagaorchestratorservice.model.enums;

import lombok.Getter;

/**
 * Enum defining command types for Order Purchase Saga
 * Simplified for graduation project: Order-Payment coordination
 */
@Getter
public enum CommandType {

    // Payment Service Commands
    PAYMENT_PROCESS("Process payment for order"),
    PAYMENT_REVERSE("Cancel/refund payment"),

    // Order Service Commands
    ORDER_UPDATE_CONFIRMED("Update order status to confirmed"),
    ORDER_UPDATE_DELIVERED("Update order status to delivered"),
    ORDER_CANCEL("Cancel order"),

    // Saga Control Commands
    START_SAGA("Start order purchase saga"),
    COMPLETE_SAGA("Complete saga successfully");

    private final String description;

    CommandType(String description) {
        this.description = description;
    }

    /**
     * Get the target service for a command type
     */
    public String getTargetService() {
        if (this.name().startsWith("PAYMENT_")) {
            return "PAYMENT_SERVICE";
        } else if (this.name().startsWith("ORDER_")) {
            return "ORDER_SERVICE";
        } else {
            return "SAGA_ORCHESTRATOR_SERVICE";
        }
    }

    /**
     * Check if this is a compensation command
     */
    public boolean isCompensationCommand() {
        return this == PAYMENT_REVERSE || this == ORDER_CANCEL;
    }
}