package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.client.UserServiceClient;
import com.graduation.schedulingservice.model.CalendarItem;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.payload.response.TimeRangeDto;
import com.graduation.schedulingservice.payload.response.UserConstraintsDTO;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConstraintValidationService {

    private final UserServiceClient userServiceClient;
    private final CalendarItemRepository calendarItemRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Validate all constraints for scheduling a calendar item
     */
    public List<String> validateConstraints(Long userId, LocalDateTime startTime,
                                            LocalDateTime endTime, ItemType itemType) {
        List<String> violations = new ArrayList<>();

        // 1. ALWAYS validate overlapping (hard rule, not user preference)
        validateOverlapping(userId, startTime, endTime, violations);

        // 2. Fetch user constraints from the User Service for sleep hours and daily limits
        Optional<UserConstraintsDTO> constraintsOpt = userServiceClient.fetchUserConstraints(userId);

        if (constraintsOpt.isEmpty()) {
            log.warn("No constraints found for user {} or User Service is down. Skipping sleep/limit validation.", userId);
            return violations; // Return only overlapping violations
        }

        UserConstraintsDTO constraints = constraintsOpt.get();

        // 3. Validate sleep hours
        validateSleepHours(constraints, startTime, endTime, violations);

        // 4. Validate daily limits (if feature is enabled)
        if (constraints.getDailyLimitFeatureEnabled()) {
            validateDailyLimit(userId, constraints, startTime, endTime, itemType, violations);
        }

        return violations;
    }

    /**
     * Validates *only* the user's configured constraints (Sleep Hours, Daily Limits).
     * This method does NOT check for overlaps.
     */
    public List<String> validateBaseConstraints(Long userId, LocalDateTime startTime,
                                                LocalDateTime endTime, ItemType itemType) {
        List<String> violations = new ArrayList<>();

        // 1. Fetch user constraints from the User Service
        Optional<UserConstraintsDTO> constraintsOpt = userServiceClient.fetchUserConstraints(userId);

        if (constraintsOpt.isEmpty()) {
            log.warn("No constraints found for user {} or User Service is down. Skipping sleep/limit validation.", userId);
            return violations; // Return empty list
        }

        UserConstraintsDTO constraints = constraintsOpt.get();

        // 2. Validate sleep hours
        validateSleepHours(constraints, startTime, endTime, violations);

        // 3. Validate daily limits (if feature is enabled)
        if (constraints.getDailyLimitFeatureEnabled() != null && constraints.getDailyLimitFeatureEnabled()) {
            validateDailyLimit(userId, constraints, startTime, endTime, itemType, violations);
        }

        return violations;
    }

    /**
     * HARD RULE: Check if the new time slot overlaps with any existing scheduled items
     * This is ALWAYS validated regardless of user preferences
     */
    private void validateOverlapping(Long userId, LocalDateTime startTime,
                                     LocalDateTime endTime, List<String> violations) {
        log.debug("Checking overlapping items for user {} between {} and {}",
                userId, startTime, endTime);

        List<CalendarItem> overlappingItems = calendarItemRepository
                .findOverlappingItems(userId, startTime, endTime);

        if (!overlappingItems.isEmpty()) {
            log.warn("Found {} overlapping items for user {}", overlappingItems.size(), userId);

            // Define the formatters for date and time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a"); // e.g., 6:30 PM
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");

            String itemNames = overlappingItems.stream()
                    .map(item -> {
                        LocalDateTime startDateTime = item.getTimeSlot().getStartTime();
                        LocalDateTime endDateTime = item.getTimeSlot().getEndTime();

                        String formattedTime;

                        // Check if start and end are on the same day
                        if (startTime.toLocalDate().isEqual(endTime.toLocalDate())) {
                            // Same day: 'Task Name' (Nov 7, 6:30 PM - 6:45 PM)
                            formattedTime = String.format("%s, %s - %s",
                                    startDateTime.format(dateFormatter),  // "Nov 7"
                                    startDateTime.format(timeFormatter), // "6:30 PM"
                                    endDateTime.format(timeFormatter)    // "6:45 PM"
                            );
                        } else {
                            // Different days: 'Task Name' (Nov 7, 11:00 PM - Nov 8, 1:00 AM)
                            formattedTime = String.format("%s - %s",
                                    startDateTime.format(fullFormatter), // "Nov 7, 11:00 PM"
                                    endDateTime.format(fullFormatter)    // "Nov 8, 1:00 AM"
                            );
                        }

                        return String.format("'%s' (%s)", item.getName(), formattedTime);
                    })
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("existing items");

            violations.add(String.format(
                    "Time slot overlaps with existing calendar items: %s",
                    itemNames));
        } else {
            log.debug("No overlapping items found for user {}", userId);
        }
    }

    /**
     * Check if start or end time falls within any sleep hour range
     */
    private void validateSleepHours(UserConstraintsDTO constraints, LocalDateTime startTime,
                                    LocalDateTime endTime, List<String> violations) {
        List<TimeRangeDto> sleepHours = constraints.getSleepHours();

        if (sleepHours == null || sleepHours.isEmpty()) {
            return;
        }

        LocalTime startLocalTime = startTime.toLocalTime();
        LocalTime endLocalTime = endTime.toLocalTime();

        for (TimeRangeDto sleepRange : sleepHours) {
            // --- FIX STARTS HERE ---
            // 1. Parse the String from DTO into LocalTime
            LocalTime sleepStart = LocalTime.parse(sleepRange.getStartTime(), TIME_FORMATTER);
            LocalTime sleepEnd = LocalTime.parse(sleepRange.getEndTime(), TIME_FORMATTER);
            // --- FIX ENDS HERE ---

            boolean startInSleep = isTimeInRange(startLocalTime, sleepStart, sleepEnd);
            boolean endInSleep = isTimeInRange(endLocalTime, sleepStart, sleepEnd);

            // Note: You also need to check if the sleep time is entirely *inside* the task time
            // (e.g. Task is 10PM-8AM, Sleep is 2AM-4AM)
            boolean sleepInsideTask = isTimeInRange(sleepStart, startLocalTime, endLocalTime);

            if (startInSleep || endInSleep || sleepInsideTask) {
                String formattedStart = sleepStart.format(DISPLAY_FORMATTER);
                String formattedEnd = sleepEnd.format(DISPLAY_FORMATTER);

                violations.add(String.format(
                        "Time slot conflicts with sleep hours (%s - %s).",
                        formattedStart,
                        formattedEnd));
                return;
            }
        }
    }


    /**
     * Helper method to check if a time falls within a range
     * Handles overnight ranges (e.g., 22:00 - 06:00)
     */
    private boolean isTimeInRange(LocalTime time, LocalTime rangeStart, LocalTime rangeEnd) {
        if (rangeStart.isAfter(rangeEnd)) { // Overnight range (e.g., 22:00 - 06:00)
            return time.isAfter(rangeStart) || time.isBefore(rangeEnd);
        }
        return !time.isBefore(rangeStart) && time.isBefore(rangeEnd);
    }

    /**
     * Check if adding this item would exceed the daily limit for the item type
     */
    private void validateDailyLimit(Long userId, UserConstraintsDTO constraints,
                                    LocalDateTime startTime, LocalDateTime endTime,
                                    ItemType itemType, List<String> violations) {

        // Get the configured daily limit for this item type
        Integer dailyLimitHours = constraints.getDailyLimits()
                .get(itemType.name());

        if (dailyLimitHours == null || dailyLimitHours <= 0) {
            return; // No limit configured for this item type
        }

        // Get all items scheduled on this date for this item type
        LocalDate date = startTime.toLocalDate();
        List<CalendarItem> itemsOnDate = calendarItemRepository
                .findAllByUserIdAndDate(userId, date);

        // Calculate total hours already used for this item type
        long totalMinutesUsed = itemsOnDate.stream()
                .filter(item -> item.getType() == itemType)
                .map(CalendarItem::getTimeSlot)
                .filter(Objects::nonNull)
                .filter(slot -> slot.getStartTime() != null && slot.getEndTime() != null)
                .mapToLong(slot -> Duration.between(
                        slot.getStartTime(),
                        slot.getEndTime()
                ).toMinutes())
                .sum();

        // Calculate the duration of the new item
        long newItemMinutes = Duration.between(startTime, endTime).toMinutes();

        // Calculate total if we add this new item
        long totalMinutesAfter = totalMinutesUsed + newItemMinutes;
        double totalHoursAfter = totalMinutesAfter / 60.0;

        // Check if it exceeds the limit
        if (totalHoursAfter > dailyLimitHours) {
            double hoursUsed = totalMinutesUsed / 60.0;
            double newItemHours = newItemMinutes / 60.0;

            violations.add(String.format(
                    "Daily limit exceeded for %s items. " +
                            "Limit: %d hours, Currently used: %.1f hours, " +
                            "New item: %.1f hours, Total would be: %.1f hours",
                    itemType.name(),
                    dailyLimitHours,
                    hoursUsed,
                    newItemHours,
                    totalHoursAfter));
        }
    }
}