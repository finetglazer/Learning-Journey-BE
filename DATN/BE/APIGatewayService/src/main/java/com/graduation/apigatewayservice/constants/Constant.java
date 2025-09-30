package com.graduation.apigatewayservice.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for hashcode exclusion fields.
 * Defines field names that should be excluded from hashcode generation
 * to prevent sensitive data exposure and improve performance.
 */
public final class Constant {

    /* Logging Messages */
    public static final Map<String, String> msg = new HashMap<>();

    /* Fields to exclude from hashcode generation for security and performance */
    public static final Set<String> HASHCODE_EXCLUSION_FIELDS = Set.of(
            // Sensitive authentication and security fields
            "password",
            "token",
            "accessToken",
            "refreshToken",
            "apiKey",
            "secretKey",
            "authorization",
            "credentials",
            "sessionId",
            "cookie",

            // Sensitive personal information
            "ssn",
            "socialSecurityNumber",
            "creditCardNumber",
            "cardNumber",
            "cvv",
            "pin",
            "bankAccount",
            "routingNumber",

            // Large objects that don't contribute to identity
            "timestamp",
            "createdAt",
            "updatedAt",
            "lastModified",
            "metadata",
            "headers",
            "stackTrace",
            "exception",

            // Frequently changing fields
            "requestId",
            "correlationId",
            "traceId",
            "spanId",
            "nonce",
            "salt",

            // Binary or large content
            "image",
            "file",
            "document",
            "content",
            "blob",
            "attachment"
    );

    /* Logging exclusion fields for sensitive data */
    public static final Set<String> LOGGING_EXCLUSION_FIELDS = Set.of(
            "password",
            "token",
            "accessToken",
            "refreshToken",
            "apiKey",
            "secretKey",
            "authorization",
            "credentials",
            "ssn",
            "socialSecurityNumber",
            "creditCardNumber",
            "cardNumber",
            "cvv",
            "pin",
            "bankAccount",
            "routingNumber"
    );

    /* Service names for consistent logging */
    public static final String ORDER_SERVICE = "Order Service";
    public static final String PAYMENT_SERVICE = "Payment Service";
    public static final String PRODUCT_SERVICE = "Product Service";
    public static final String USER_SERVICE = "User Service";

    static {
        // Fallback messages
        msg.put("ORDER_SERVICE_FALLBACK", "Order Service is currently unavailable. Please try again later.");
        msg.put("PAYMENT_SERVICE_FALLBACK", "Payment Service is currently unavailable. Please try again later.");
        msg.put("PRODUCT_SERVICE_FALLBACK", "Product Service is currently unavailable. Please try again later.");
        msg.put("USER_SERVICE_FALLBACK", "User Service is currently unavailable. Please try again later.");

        // Error messages
        msg.put("GENERIC_ERROR", "An unexpected error occurred. Please try again later.");
        msg.put("TIMEOUT_ERROR", "Request timed out. Please try again.");
        msg.put("CIRCUIT_BREAKER_OPEN", "Service is temporarily unavailable due to high error rate.");

        // Success messages
        msg.put("SERVICE_RECOVERED", "Service has recovered and is now available.");
        msg.put("FALLBACK_SUCCESS", "Fallback response delivered successfully.");
    }

    /**
     * Utility method to check if a field should be excluded from hashcode
     */
    public static boolean shouldExcludeFromHashcode(String fieldName) {
        return HASHCODE_EXCLUSION_FIELDS.contains(fieldName);
    }

    /**
     * Utility method to check if a field should be excluded from logging
     */
    public static boolean shouldExcludeFromLogging(String fieldName) {
        return LOGGING_EXCLUSION_FIELDS.contains(fieldName);
    }

    /* HTTP Status related constants */
    public static final int SERVICE_UNAVAILABLE_CODE = 503;
    public static final int TIMEOUT_CODE = 408;
    public static final int INTERNAL_ERROR_CODE = 500;

    /* Circuit breaker states */
    public static final String CIRCUIT_BREAKER_OPEN = "OPEN";
    public static final String CIRCUIT_BREAKER_CLOSED = "CLOSED";
    public static final String CIRCUIT_BREAKER_HALF_OPEN = "HALF_OPEN";

    // Private constructor to prevent instantiation
    private Constant() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}