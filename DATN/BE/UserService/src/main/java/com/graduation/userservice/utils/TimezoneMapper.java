package com.graduation.userservice.utils;

import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to manage and format IANA timezone IDs for display.
 * It dynamically generates display strings to ensure UTC offsets are always correct,
 * accounting for Daylight Saving Time.
 */
@Service
public class TimezoneMapper {

    // The single source of truth: A curated list of supported IANA zone IDs.
    private static final Set<String> SUPPORTED_IANA_IDS = Set.of(
            // Asia
            "Asia/Bangkok", "Asia/Ho_Chi_Minh", "Asia/Jakarta", "Asia/Singapore",
            "Asia/Hong_Kong", "Asia/Manila", "Asia/Tokyo", "Asia/Seoul",
            "Asia/Kolkata", "Asia/Shanghai", "Asia/Dubai",
            // Americas
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
            "America/Sao_Paulo", "America/Toronto", "America/Mexico_City",
            // Europe
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Rome",
            "Europe/Madrid", "Europe/Athens", "Europe/Moscow",
            // Oceania
            "Australia/Sydney", "Australia/Melbourne", "Pacific/Auckland",
            // Africa
            "Africa/Cairo", "Africa/Johannesburg",
            // UTC
            "UTC"
    );

    /**
     * Generates a list of user-friendly timezone display strings.
     * The UTC offset is calculated dynamically to be correct for the current date.
     * Format: "UTC-04:00 New York" (correctly shows DST)
     *
     * @return A sorted list of display strings.
     */
    public List<String> getSupportedTimezonesForDisplay() {
        return SUPPORTED_IANA_IDS.stream()
                .map(this::toDisplayFormat)
                .sorted(Comparator.naturalOrder()) // Sort for a clean UI dropdown
                .collect(Collectors.toList());
    }

    /**
     * Converts an IANA timezone ID to its dynamic display format.
     *
     * @param ianaId e.g., "America/New_York"
     * @return Display format e.g., "UTC-04:00 New York"
     */
    public String toDisplayFormat(String ianaId) {
        if (ianaId == null || !isValidIanaId(ianaId)) {
            // Return a sensible default
            return "UTC+00:00 UTC";
        }

        if ("UTC".equalsIgnoreCase(ianaId)) {
            return "UTC+00:00 UTC";
        }

        ZoneId zoneId = ZoneId.of(ianaId);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String offset = now.getOffset().toString();
        // For display, replace underscores and remove the region prefix.
        String friendlyName = ianaId.substring(ianaId.indexOf('/') + 1).replace('_', ' ');

        return String.format("UTC%s %s", "Z".equals(offset) ? "+00:00" : offset, friendlyName);
    }

    /**
     * Converts a display format string back to its IANA ID.
     * Note: This is less reliable due to dynamic offsets. The best practice is for the
     * client to send the IANA ID directly. This method is provided for compatibility.
     *
     * @param displayFormat e.g., "UTC-04:00 New York"
     * @return IANA ID e.g., "America/New_York"
     * @throws IllegalArgumentException if the format is not recognized.
     */
    public String toIanaId(String displayFormat) {
        if (displayFormat == null || displayFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Timezone cannot be null or empty");
        }

        // This is a simplified lookup. A more robust solution might involve parsing.
        // The best approach is to have the frontend send the IANA ID.
        return SUPPORTED_IANA_IDS.stream()
                .filter(ianaId -> toDisplayFormat(ianaId).equals(displayFormat))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported timezone format: " + displayFormat));
    }


    /**
     * Validates if the IANA ID is in our supported list.
     *
     * @param ianaId e.g., "Asia/Bangkok"
     * @return true if supported, false otherwise
     */
    public boolean isValidIanaId(String ianaId) {
        return ianaId != null && SUPPORTED_IANA_IDS.contains(ianaId);
    }
}