package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.*;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.TimeSlotDTO;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.payload.response.CreateItemResponse;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.service.CalendarItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.time.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarItemServiceImpl implements CalendarItemService {

    private final CalendarItemRepository calendarItemRepository;
    private final CalendarRepository calendarRepository;

    @Override
    @Transactional
    public BaseResponse<?> createItem(Long userId, CreateCalendarItemRequest request) {
        try {
            // 1. Validate item type
            ItemType itemType;
            try {
                itemType = ItemType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid item type provided: {}", request.getType());
                return new BaseResponse<>(0, Constant.MSG_INVALID_ITEM_TYPE, null);
            }

            // 2. Validate time slot if provided
            if (request.getTimeSlot() != null) {
                TimeSlotDTO timeSlotDTO = request.getTimeSlot();

                // Validate timezone format
                try {
                    ZoneId.of(timeSlotDTO.getTimezone());
                } catch (DateTimeException e) {
                    log.warn("Invalid timezone format: {}", timeSlotDTO.getTimezone());
                    return new BaseResponse<>(0, "Invalid timezone format", null);
                }

                // Validate start < end
                if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                        timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                    log.warn(Constant.LOG_INVALID_TIME_SLOT,
                            timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_SLOT, null);
                }
            }

            if (!calendarRepository.existsByIdAndUserId(request.getCalendarId(), userId)) {
                log.warn("Calendar not found or unauthorized: calendarId={}, userId={}",
                        request.getCalendarId(), userId);
                return new BaseResponse<>(0, Constant.MSG_CALENDAR_NOT_FOUND, null);
            }

            // TODO: Validate constraints (sleep hours, daily limits, overlapping)
            // if (request.getTimeSlot() != null) {
            //     List<ConstraintViolation> violations =
            //         constraintValidator.validate(userId, timeSlot, itemType);
            //     if (!violations.isEmpty()) {
            //         return new BaseResponse<>(0, "Constraint violations", violations);
            //     }
            // }

            // 3. Create calendar item based on type
            CalendarItem calendarItem;
            switch (itemType) {
                case TASK:
                    calendarItem = createTask(userId, request);
                    break;
                case ROUTINE:
                    calendarItem = createRoutine(userId, request);
                    break;
                case EVENT:
                    calendarItem = createEvent(userId, request);
                    break;
                default:
                    return new BaseResponse<>(0, Constant.MSG_INVALID_ITEM_TYPE, null);
            }

            // 4. Save to database
            CalendarItem savedItem = calendarItemRepository.save(calendarItem);

            // 5. Return success response
            CreateItemResponse response = new CreateItemResponse(
                    true,
                    savedItem.getId(),
                    Constant.MSG_ITEM_CREATED_SUCCESS
            );

            log.info(Constant.LOG_ITEM_CREATED_SUCCESS, userId, savedItem.getId(), itemType);
            return new BaseResponse<>(1, Constant.MSG_ITEM_CREATED_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_ITEM_CREATION_FAILED, userId, e);
            return new BaseResponse<>(0, "Failed to create calendar item", null);
        }
    }

    /**
     * Create a Task item (SIMPLIFIED - no priority)
     */
    private Task createTask(Long userId, CreateCalendarItemRequest request) {
        Task task = new Task();

        // Set common fields
        setCommonFields(task, userId, request);

        // Set task-specific fields
        if (request.getTaskDetails() != null) {
            task.setEstimatedHours(request.getTaskDetails().getEstimatedHours());
            task.setDueDate(request.getTaskDetails().getDueDate());
        }

        return task;
    }

    /**
     * Create a Routine item - UPDATED: Simplified pattern (week-based only)
     */
    private Routine createRoutine(Long userId, CreateCalendarItemRequest request) {
        Routine routine = new Routine();

        // Set common fields
        setCommonFields(routine, userId, request);

        // Set routine-specific fields
        if (request.getRoutineDetails() != null &&
                request.getRoutineDetails().getPattern() != null) {

            RecurringPattern pattern = new RecurringPattern();
            var patternDTO = request.getRoutineDetails().getPattern();

            // Parse and validate days of week
            if (patternDTO.getDaysOfWeek() != null && !patternDTO.getDaysOfWeek().isEmpty()) {
                var daysOfWeek = patternDTO.getDaysOfWeek().stream()
                        .map(day -> {
                            try {
                                return DayOfWeek.valueOf(day.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid day of week: {}", day);
                                return null;
                            }
                        })
                        .filter(day -> day != null)
                        .collect(Collectors.toList());

                if (daysOfWeek.isEmpty()) {
                    log.warn("No valid days of week provided for routine");
                    // Set default to Monday if no valid days
                    daysOfWeek.add(DayOfWeek.MONDAY);
                }

                pattern.setDaysOfWeek(daysOfWeek);
            } else {
                log.warn("Days of week is required for routine pattern");
                // Set default to Monday
                pattern.setDaysOfWeek(java.util.Collections.singletonList(DayOfWeek.MONDAY));
            }

            routine.setPattern(pattern);
        }

        return routine;
    }

    /**
     * Create an Event item (SIMPLIFIED - no event type)
     */
    private Event createEvent(Long userId, CreateCalendarItemRequest request) {
        Event event = new Event();

        // Set common fields
        setCommonFields(event, userId, request);

        // Set event-specific fields
        if (request.getEventDetails() != null) {
            event.setLocation(request.getEventDetails().getLocation());
            event.setIsAllDay(request.getEventDetails().getIsAllDay() != null ?
                    request.getEventDetails().getIsAllDay() : false);

            // Set attendees
            if (request.getEventDetails().getAttendees() != null) {
                request.getEventDetails().getAttendees().forEach(event::addAttendee);
            }
        }

        return event;
    }

    /**
     * Set common fields for all calendar item types
     */
    private void setCommonFields(CalendarItem item, Long userId, CreateCalendarItemRequest request) {
        item.setUserId(userId);
        item.setCalendarId(request.getCalendarId());
        item.setName(request.getName());
        item.setNote(request.getNote());
        item.setColor(request.getColor());
        item.setStatus(ItemStatus.INCOMPLETE);

        // Set time slot if provided
        if (request.getTimeSlot() != null) {
            TimeSlot timeSlot = new TimeSlot(
                    request.getTimeSlot().getStartTime(),
                    request.getTimeSlot().getEndTime(),
                    request.getTimeSlot().getTimezone()
            );
            item.setTimeSlot(timeSlot);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> convertUserTimezone(Long userId, String oldTimezone, String newTimezone) {
        try {
            log.info("Converting timezone for user {} from {} to {}", userId, oldTimezone, newTimezone);

            // 1. Validate timezone formats
            ZoneId oldZone;
            ZoneId newZone;
            try {
                oldZone = ZoneId.of(oldTimezone);
                newZone = ZoneId.of(newTimezone);
            } catch (DateTimeException e) {
                log.warn("Invalid timezone format: old={}, new={}", oldTimezone, newTimezone);
                return new BaseResponse<>(0, "Invalid timezone format", null);
            }

            // 2. Get all calendar items for the user
            List<CalendarItem> items = calendarItemRepository.findAllByUserId(userId);

            if (items.isEmpty()) {
                log.info("No calendar items found for user {}", userId);
                return new BaseResponse<>(1, "No calendar items to convert", null);
            }

            // 3. Convert each item's timeSlot
            int convertedCount = 0;
            for (CalendarItem item : items) {
                TimeSlot timeSlot = item.getTimeSlot();

                // Skip items without time slots (unscheduled items)
                if (timeSlot == null || timeSlot.getStartTime() == null) {
                    continue;
                }

                // Convert start and end times maintaining absolute time
                // Option B: 2PM Bangkok (UTC+7) becomes 2AM New York (UTC-5)
                LocalDateTime oldStart = timeSlot.getStartTime();
                LocalDateTime oldEnd = timeSlot.getEndTime();

                // Convert to absolute instant, then to new timezone
                ZonedDateTime oldStartZoned = oldStart.atZone(oldZone);
                ZonedDateTime oldEndZoned = oldEnd.atZone(oldZone);

                ZonedDateTime newStartZoned = oldStartZoned.withZoneSameInstant(newZone);
                ZonedDateTime newEndZoned = oldEndZoned.withZoneSameInstant(newZone);

                // Update the time slot
                timeSlot.setStartTime(newStartZoned.toLocalDateTime());
                timeSlot.setEndTime(newEndZoned.toLocalDateTime());
                timeSlot.setTimezone(newTimezone);

                convertedCount++;
            }

            // 4. Save all changes
            calendarItemRepository.saveAll(items);

            log.info("Successfully converted {} calendar items for user {}", convertedCount, userId);
            return new BaseResponse<>(1,
                    String.format("Successfully converted %d calendar items", convertedCount),
                    null);

        } catch (Exception e) {
            log.error("Failed to convert timezone for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to convert calendar items timezone", null);
        }
    }
}