package com.graduation.userservice.utils;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for generating unique saga identifiers
 */
public class SagaIdGenerator {

    private static final String SAGA_PREFIX = "SAGA";
    private static final String SEPARATOR = "-";

    /**
     * Generate a unique saga ID with timestamp and UUID
     * Format: SAGA-{timestamp}-{uuid}
     * Example: SAGA-1642678800000-a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static String generate() {
        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return SAGA_PREFIX + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Generate a saga ID with a specific prefix
     * Format: {prefix}-SAGA-{timestamp}-{uuid}
     * Example: ORDER-SAGA-1642678800000-a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static String generateWithPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generate();
        }

        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return prefix.toUpperCase() + SEPARATOR + SAGA_PREFIX + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Generate a short saga ID (without full UUID)
     * Format: SAGA-{timestamp}-{shortId}
     * Example: SAGA-1642678800000-a1b2c3d4
     */
    public static String generateShort() {
        long timestamp = Instant.now().toEpochMilli();
        String shortId = UUID.randomUUID().toString().substring(0, 8);

        return SAGA_PREFIX + SEPARATOR + timestamp + SEPARATOR + shortId;
    }

    /**
     * Generate a saga ID for a specific saga type
     * Format: {sagaType}-{timestamp}-{uuid}
     * Example: ORDER-PURCHASE-1642678800000-a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static String generateForType(String sagaType) {
        if (sagaType == null || sagaType.trim().isEmpty()) {
            return generate();
        }

        long timestamp = Instant.now().toEpochMilli();
        String uuid = UUID.randomUUID().toString();

        return sagaType.toUpperCase().replace(" ", "_") + SEPARATOR + timestamp + SEPARATOR + uuid;
    }

    /**
     * Validate if a string is a valid saga ID format
     */
    public static boolean isValidSagaId(String sagaId) {
        if (sagaId == null || sagaId.trim().isEmpty()) {
            return false;
        }

        // Check if it contains the basic structure with separators
        String[] parts = sagaId.split(SEPARATOR);

        // Minimum format: PREFIX-TIMESTAMP-UUID (3 parts)
        if (parts.length < 3) {
            return false;
        }

        // Check if timestamp part is numeric
        try {
            Long.parseLong(parts[parts.length - 2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extract timestamp from saga ID
     */
    public static Long extractTimestamp(String sagaId) {
        if (!isValidSagaId(sagaId)) {
            return null;
        }

        try {
            String[] parts = sagaId.split(SEPARATOR);
            return Long.parseLong(parts[parts.length - 2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Extract creation time as Instant from saga ID
     */
    public static Instant extractCreationTime(String sagaId) {
        Long timestamp = extractTimestamp(sagaId);
        return timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
    }
}