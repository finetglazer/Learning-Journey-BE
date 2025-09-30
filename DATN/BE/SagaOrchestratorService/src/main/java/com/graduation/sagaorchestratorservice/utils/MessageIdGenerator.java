package com.graduation.sagaorchestratorservice.utils;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for generating unique message identifiers
 */
public class MessageIdGenerator {

    private static final String MESSAGE_PREFIX = "MSG";
    private static final String SEPARATOR = "-";

    /**
     * Generate a unique message ID
     * Format: MSG-{timestamp}-{uuid}
     * Example: MSG-1642678800000-a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static String generate() {
        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return MESSAGE_PREFIX + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Generate a message ID with a specific type prefix
     * Format: {messageType}-MSG-{timestamp}-{uuid}
     * Example: CMD-MSG-1642678800000-a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static String generateWithType(String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            return generate();
        }

        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return messageType.toUpperCase() + SEPARATOR + MESSAGE_PREFIX + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Generate a message ID for a specific saga and step
     * Format: MSG-{sagaId}-{stepId}-{shortUuid}
     * This ensures uniqueness per saga step for idempotency
     */
    public static String generateForSagaStep(String sagaId, Integer stepId) {
        if (sagaId == null || stepId == null) {
            return generate();
        }

        // Use deterministic UUID based on sagaId and stepId for idempotency
        String input = sagaId + "-" + stepId;
        String deterministicId = UUID.nameUUIDFromBytes(input.getBytes()).toString().substring(0, 8);

        return MESSAGE_PREFIX + SEPARATOR + sagaId + SEPARATOR + stepId + SEPARATOR + deterministicId;
    }

    /**
     * Generate a short message ID (without full UUID)
     * Format: MSG-{timestamp}-{shortId}
     * Example: MSG-1642678800000-a1b2c3d4
     */
    public static String generateShort() {
        long timestamp = Instant.now().toEpochMilli();
        String shortId = UUID.randomUUID().toString().substring(0, 8);

        return MESSAGE_PREFIX + SEPARATOR + timestamp + SEPARATOR + shortId;
    }

    /**
     * Generate a correlation ID for tracking related messages
     * Format: CORR-{timestamp}-{uuid}
     */
    public static String generateCorrelationId() {
        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return "CORR" + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Generate a message ID for command messages
     * Format: CMD-{timestamp}-{uuid}
     */
    public static String generateCommandId() {
        return generateWithType("CMD");
    }

    /**
     * Generate a message ID for event messages
     * Format: EVT-{timestamp}-{uuid}
     */
    public static String generateEventId() {
        return generateWithType("EVT");
    }

    /**
     * Validate if a string is a valid message ID format
     */
    public static boolean isValidMessageId(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }

        // Check if it contains the basic structure with separators
        String[] parts = messageId.split(SEPARATOR);

        // Minimum format: PREFIX-TIMESTAMP-UUID (3 parts)
        return parts.length >= 3;
    }

    /**
     * Extract timestamp from message ID
     */
    public static Long extractTimestamp(String messageId) {
        if (!isValidMessageId(messageId)) {
            return null;
        }

        try {
            String[] parts = messageId.split(SEPARATOR);

            // Try to find timestamp part (usually second part for standard format)
            for (int i = 1; i < parts.length; i++) {
                try {
                    return Long.parseLong(parts[i]);
                } catch (NumberFormatException e) {
                    // Continue to next part
                }
            }
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Extract creation time as Instant from message ID
     */
    public static Instant extractCreationTime(String messageId) {
        Long timestamp = extractTimestamp(messageId);
        return timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
    }

    /**
     * Extract saga ID from a saga-step message ID
     */
    public static String extractSagaId(String messageId) {
        if (!isValidMessageId(messageId) || !messageId.startsWith(MESSAGE_PREFIX + SEPARATOR)) {
            return null;
        }

        String[] parts = messageId.split(SEPARATOR);
        // Format: MSG-{sagaId}-{stepId}-{shortUuid}
        if (parts.length >= 4) {
            return parts[1]; // sagaId is the second part
        }

        return null;
    }

    /**
     * Extract step ID from a saga-step message ID
     */
    public static Integer extractStepId(String messageId) {
        if (!isValidMessageId(messageId) || !messageId.startsWith(MESSAGE_PREFIX + SEPARATOR)) {
            return null;
        }

        try {
            String[] parts = messageId.split(SEPARATOR);
            // Format: MSG-{sagaId}-{stepId}-{shortUuid}
            if (parts.length >= 4) {
                return Integer.parseInt(parts[2]); // stepId is the third part
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }
}