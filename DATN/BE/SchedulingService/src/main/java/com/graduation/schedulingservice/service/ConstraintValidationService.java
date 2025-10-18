package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.client.UserServiceClient;
import com.graduation.schedulingservice.model.CalendarItem;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.payload.response.TimeRangeDTO;
import com.graduation.schedulingservice.payload.response.UserConstraintsDTO;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

            String itemNames = overlappingItems.stream()
                    .map(item -> String.format("'%s' (%s - %s)",
                            item.getName(),
                            item.getTimeSlot().getStartTime(),
                            item.getTimeSlot().getEndTime()))
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
        List<TimeRangeDTO> sleepHours = constraints.getSleepHours();

        if (sleepHours == null || sleepHours.isEmpty()) {
            return; // No sleep hours configured
        }

        LocalTime startLocalTime = startTime.toLocalTime();
        LocalTime endLocalTime = endTime.toLocalTime();

        for (TimeRangeDTO sleepRange : sleepHours) {
            boolean startInSleep = isTimeInRange(startLocalTime, sleepRange.getStartTime(), sleepRange.getEndTime());
            boolean endInSleep = isTimeInRange(endLocalTime, sleepRange.getStartTime(), sleepRange.getEndTime());

            if (startInSleep || endInSleep) {
                violations.add(String.format(
                        "Time slot conflicts with sleep hours (%s - %s).",
                        sleepRange.getStartTime(),
                        sleepRange.getEndTime()));
                return; // One violation is enough
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