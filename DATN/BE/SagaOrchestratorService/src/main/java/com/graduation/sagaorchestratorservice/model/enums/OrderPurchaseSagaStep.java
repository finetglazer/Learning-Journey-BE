package com.graduation.sagaorchestratorservice.model.enums;

import lombok.Getter;

/**
 * Enum defining all steps in the Order Purchase Saga
 * Updated workflow: Order (CREATED) → Payment → Order (CONFIRMED) → Order (DELIVERED) → Complete
 */
@Getter
public enum OrderPurchaseSagaStep {

    // Normal flow steps (starting after order is already CREATED)
    PROCESS_PAYMENT(1, "Process payment for order", CommandType.PAYMENT_PROCESS),
    UPDATE_ORDER_STATUS_CONFIRMED(2, "Update order status to confirmed", CommandType.ORDER_UPDATE_CONFIRMED),
    UPDATE_ORDER_STATUS_DELIVERED(3, "Update order status to delivered", CommandType.ORDER_UPDATE_DELIVERED),
    COMPLETE_SAGA(4, "Complete saga successfully", CommandType.COMPLETE_SAGA),

    // Compensation steps (reverse order)
    CANCEL_PAYMENT(101, "Cancel/refund payment", CommandType.PAYMENT_REVERSE),
    CANCEL_ORDER(102, "Cancel order", CommandType.ORDER_CANCEL);

    private final int stepNumber;
    private final String description;
    private final CommandType commandType;

    OrderPurchaseSagaStep(int stepNumber, String description, CommandType commandType) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.commandType = commandType;
    }

    /**
     * Get the next step in normal flow
     */
    public OrderPurchaseSagaStep getNextStep() {
        return switch (this) {
            case PROCESS_PAYMENT -> UPDATE_ORDER_STATUS_CONFIRMED;
            case UPDATE_ORDER_STATUS_CONFIRMED -> UPDATE_ORDER_STATUS_DELIVERED;
            case UPDATE_ORDER_STATUS_DELIVERED -> COMPLETE_SAGA;
            case COMPLETE_SAGA -> null; // End of saga
            default -> null; // Compensation steps don't have "next"
        };
    }

    /**
     * Get the next step in compensation flow
     */
    public OrderPurchaseSagaStep getNextCompensationStep() {
        return switch (this) {
            case CANCEL_PAYMENT -> CANCEL_ORDER;
            case CANCEL_ORDER -> COMPLETE_SAGA; // End compensation
            default -> null;
        };
    }

    /**
     * Determine the first compensation step based on completed steps
     */
    public static OrderPurchaseSagaStep determineFirstCompensationStep(
            boolean paymentProcessed,
            boolean orderStatusConfirmed,
            boolean orderStatusDelivered) {

        if (orderStatusDelivered) {
            // If order was delivered, we don't need to cancel it for payment failure
            // Just cancel the payment
            return CANCEL_PAYMENT;
        } else if (orderStatusConfirmed) {
            // If order was confirmed but not delivered, cancel payment first, then order
            return CANCEL_PAYMENT;
        } else if (paymentProcessed) {
            // Payment went through but order update failed
            // Cancel payment first, then order
            return CANCEL_PAYMENT;
        } else {
            // Payment failed, just cancel the order
            return CANCEL_ORDER;
        }
    }

    /**
     * Legacy method for backward compatibility - now with 3 parameters
     */
    public static OrderPurchaseSagaStep determineFirstCompensationStep(
            boolean paymentProcessed,
            boolean orderStatusUpdated) {
        // Treat the old orderStatusUpdated as orderStatusConfirmed
        return determineFirstCompensationStep(paymentProcessed, orderStatusUpdated, false);
    }

    /**
     * Check if this is a compensation step
     */
    public boolean isCompensationStep() {
        return stepNumber >= 100;
    }

    /**
     * Get step by step number
     */
    public static OrderPurchaseSagaStep getByStepNumber(int stepNumber) {
        for (OrderPurchaseSagaStep step : OrderPurchaseSagaStep.values()) {
            if (step.getStepNumber() == stepNumber) {
                return step;
            }
        }
        return null;
    }

    /**
     * Check if step requires compensation when failed
     */
    public boolean requiresCompensation() {
        return this == PROCESS_PAYMENT ||
                this == UPDATE_ORDER_STATUS_CONFIRMED ||
                this == UPDATE_ORDER_STATUS_DELIVERED;
    }
}