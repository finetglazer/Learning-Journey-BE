package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.*;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.TimeSlotDTO;
import com.graduation.schedulingservice.payload.request.UpdateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.service.CalendarItemService;
import com.graduation.schedulingservice.service.ConstraintValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarItemServiceImpl implements CalendarItemService {

    private final ConstraintValidationService constraintValidationService;
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

                // Validate start < end
                if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                        timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                    log.warn(Constant.LOG_INVALID_TIME_SLOT,
                            timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_SLOT, null);
                }
            }

            // 3. Validate calendar ownership
            if (!calendarRepository.existsByIdAndUserId(request.getCalendarId(), userId)) {
                log.warn("Calendar not found or unauthorized: calendarId={}, userId={}",
                        request.getCalendarId(), userId);
                return new BaseResponse<>(0, Constant.MSG_CALENDAR_NOT_FOUND, null);
            }

            // 4. Validate constraints (overlapping, sleep hours, daily limits)
            if (request.getTimeSlot() != null) {
                List<String> violations = constraintValidationService.validateConstraints(
                        userId,
                        request.getTimeSlot().getStartTime(),
                        request.getTimeSlot().getEndTime(),
                        itemType
                );

                if (!violations.isEmpty()) {
                    log.warn("Constraint violations for user {}: {}", userId, violations);
                    return new BaseResponse<>(0, "Constraint violations detected", violations);
                }
            }

            // 5. Create calendar item based on type
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

            // 6. Save to database
            CalendarItem savedItem = calendarItemRepository.save(calendarItem);

            // 7. Return success response
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
     * Create a Task item
     */
    private Task createTask(Long userId, CreateCalendarItemRequest request) {
        Task task = new Task();
        setCommonFields(task, userId, request);

        if (request.getTaskDetails() != null) {
            task.setEstimatedHours(request.getTaskDetails().getEstimatedHours());
            task.setDueDate(request.getTaskDetails().getDueDate());
        }

        return task;
    }

    /**
     * Create a Routine item
     */
    private Routine createRoutine(Long userId, CreateCalendarItemRequest request) {
        Routine routine = new Routine();
        setCommonFields(routine, userId, request);

        if (request.getRoutineDetails() != null &&
                request.getRoutineDetails().getPattern() != null) {

            RecurringPattern pattern = new RecurringPattern();
            var patternDTO = request.getRoutineDetails().getPattern();

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
                    daysOfWeek.add(DayOfWeek.MONDAY);
                }

                pattern.setDaysOfWeek(daysOfWeek);
            } else {
                pattern.setDaysOfWeek(java.util.Collections.singletonList(DayOfWeek.MONDAY));
            }

            routine.setPattern(pattern);
        }

        return routine;
    }

    /**
     * Create an Event item
     */
    private Event createEvent(Long userId, CreateCalendarItemRequest request) {
        Event event = new Event();
        setCommonFields(event, userId, request);
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

        if (request.getTimeSlot() != null) {
            TimeSlotDTO dto = request.getTimeSlot();
            TimeSlot timeSlot = new TimeSlot(
                    dto.getStartTime(),
                    dto.getEndTime()
            );
            item.setTimeSlot(timeSlot);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> convertUserTimezone(Long userId, String oldTimezone, String newTimezone) {
        try {
            log.info("Converting timezone for user {} from {} to {}", userId, oldTimezone, newTimezone);

            ZoneId oldZone;
            ZoneId newZone;
            try {
                oldZone = ZoneId.of(oldTimezone);
                newZone = ZoneId.of(newTimezone);
            } catch (DateTimeException e) {
                log.warn("Invalid timezone format: old={}, new={}", oldTimezone, newTimezone);
                return new BaseResponse<>(0, "Invalid timezone format", null);
            }

            List<CalendarItem> items = calendarItemRepository.findAllByUserId(userId);

            if (items.isEmpty()) {
                log.info("No calendar items found for user {}", userId);
                return new BaseResponse<>(1, "No calendar items to convert", null);
            }

            int convertedCount = 0;
            for (CalendarItem item : items) {
                TimeSlot timeSlot = item.getTimeSlot();

                if (timeSlot == null || timeSlot.getStartTime() == null) {
                    continue;
                }

                LocalDateTime oldStart = timeSlot.getStartTime();
                LocalDateTime oldEnd = timeSlot.getEndTime();

                ZonedDateTime oldStartZoned = oldStart.atZone(oldZone);
                ZonedDateTime oldEndZoned = oldEnd.atZone(oldZone);

                ZonedDateTime newStartZoned = oldStartZoned.withZoneSameInstant(newZone);
                ZonedDateTime newEndZoned = oldEndZoned.withZoneSameInstant(newZone);

                timeSlot.setStartTime(newStartZoned.toLocalDateTime());
                timeSlot.setEndTime(newEndZoned.toLocalDateTime());

                convertedCount++;
            }

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

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getItemById(Long userId, Long itemId) {
        try {
            log.info("Fetching calendar item: userId={}, itemId={}", userId, itemId);

            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn("Calendar item not found: itemId={}", itemId);
                return new BaseResponse<>(0, "Calendar item not found", null);
            }

            CalendarItem item = itemOpt.get();

            // Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn("Unauthorized access attempt: userId={}, itemId={}, ownerId={}",
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, "Unauthorized access", null);
            }

            // Convert to DTO
            CalendarItemResponseDTO responseDTO = convertToResponseDTO(item);

            log.info("Calendar item fetched successfully: itemId={}", itemId);
            return new BaseResponse<>(1, "Calendar item retrieved successfully", responseDTO);

        } catch (Exception e) {
            log.error("Failed to fetch calendar item: itemId={}", itemId, e);
            return new BaseResponse<>(0, "Failed to fetch calendar item", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateItem(Long userId, Long itemId, UpdateCalendarItemRequest request) {
        try {
            log.info("Updating calendar item: userId={}, itemId={}", userId, itemId);

            // 1. Find existing item
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn("Calendar item not found: itemId={}", itemId);
                return new BaseResponse<>(0, "Calendar item not found", null);
            }

            CalendarItem item = itemOpt.get();

            // 2. Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn("Unauthorized update attempt: userId={}, itemId={}, ownerId={}",
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, "Unauthorized access", null);
            }

            // 2.1 Validate name of item not null or empty
            if (request.getName() != null && request.getName().trim().isEmpty()) {
                log.warn("Invalid name provided for itemId={}", itemId);
                return new BaseResponse<>(0, "Item name cannot be empty", null);
            }

            // 3. Validate new time slot if provided
            if (request.getTimeSlot() != null) {
                TimeSlotDTO timeSlotDTO = request.getTimeSlot();

                // Validate start < end
                if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                        timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                    log.warn("Invalid time slot: start={}, end={}",
                            timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime());
                    return new BaseResponse<>(0, "Invalid time slot: end time must be after start time", null);
                }

                // 4. Validate constraints (excluding current item from overlap check)
                List<String> violations = validateConstraintsForUpdate(
                        userId,
                        itemId,
                        timeSlotDTO.getStartTime(),
                        timeSlotDTO.getEndTime(),
                        item.getType()
                );

                if (!violations.isEmpty()) {
                    log.warn("Constraint violations for user {}: {}", userId, violations);
                    return new BaseResponse<>(0, "Constraint violations detected", violations);
                }
            }

            // 5. Update common fields
            if (request.getName() != null) {
                item.setName(request.getName());
            }
            if (request.getNote() != null) {
                item.setNote(request.getNote());
            }
            if (request.getColor() != null) {
                item.setColor(request.getColor());
            }
            if (request.getStatus() != null) {
                try {
                    item.setStatus(ItemStatus.valueOf(request.getStatus().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", request.getStatus());
                    return new BaseResponse<>(0, "Invalid status value", null);
                }
            }
            if (request.getTimeSlot() != null) {
                TimeSlotDTO dto = request.getTimeSlot();
                TimeSlot timeSlot = new TimeSlot(dto.getStartTime(), dto.getEndTime());
                item.setTimeSlot(timeSlot);
            }

            // 6. Update type-specific fields
            if (item instanceof Task && request.getTaskDetails() != null) {
                Task task = (Task) item;
                if (request.getTaskDetails().getEstimatedHours() != null) {
                    task.setEstimatedHours(request.getTaskDetails().getEstimatedHours());
                }
                if (request.getTaskDetails().getDueDate() != null) {
                    task.setDueDate(request.getTaskDetails().getDueDate());
                }
            } else if (item instanceof Routine && request.getRoutineDetails() != null) {
                Routine routine = (Routine) item;
                if (request.getRoutineDetails().getPattern() != null) {
                    var patternDTO = request.getRoutineDetails().getPattern();
                    RecurringPattern pattern = routine.getPattern();

                    if (pattern == null) {
                        pattern = new RecurringPattern();
                        routine.setPattern(pattern);
                    }

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

                        pattern.setDaysOfWeek(daysOfWeek);
                    }
                }
            }

            // 7. Save updated item
            CalendarItem updatedItem = calendarItemRepository.save(item);

            log.info("Calendar item updated successfully: itemId={}", itemId);
            return new BaseResponse<>(1, "Item updated successfully", updatedItem.getId());

        } catch (Exception e) {
            log.error("Failed to update calendar item: itemId={}", itemId, e);
            return new BaseResponse<>(0, "Failed to update calendar item", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteItem(Long userId, Long itemId) {
        try {
            log.info("Deleting calendar item: userId={}, itemId={}", userId, itemId);

            // 1. Find existing item
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn("Calendar item not found: itemId={}", itemId);
                return new BaseResponse<>(0, "Calendar item not found", null);
            }

            CalendarItem item = itemOpt.get();

            // 2. Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn("Unauthorized delete attempt: userId={}, itemId={}, ownerId={}",
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, "Unauthorized access", null);
            }

            // 3. Delete the item
            calendarItemRepository.delete(item);

            log.info("Calendar item deleted successfully: itemId={}", itemId);
            return new BaseResponse<>(1, "Item deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete calendar item: itemId={}", itemId, e);
            return new BaseResponse<>(0, "Failed to delete calendar item", null);
        }
    }

    /**
     * Validate constraints for update operation
     * Excludes the current item from overlap checking
     */
    private List<String> validateConstraintsForUpdate(Long userId, Long itemId,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      ItemType itemType) {
        List<String> violations = constraintValidationService.validateConstraints(
                userId, startTime, endTime, itemType);

        // Filter out the current item from overlapping violations
        List<CalendarItem> overlappingItems = calendarItemRepository
                .findOverlappingItems(userId, startTime, endTime);

        // Remove the violation if only overlapping with itself
        if (overlappingItems.size() == 1 && overlappingItems.get(0).getId().equals(itemId)) {
            violations.removeIf(v -> v.contains("overlaps with existing calendar items"));
        }

        return violations;
    }

    /**
     * Convert CalendarItem entity to response DTO
     */
    private CalendarItemResponseDTO convertToResponseDTO(CalendarItem item) {
        CalendarItemResponseDTO dto = new CalendarItemResponseDTO();

        // Common fields
        dto.setId(item.getId());
        dto.setType(item.getType().name());
        dto.setName(item.getName());
        dto.setNote(item.getNote());
        dto.setColor(item.getColor());
        dto.setStatus(item.getStatus().name());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        // Time slot
        if (item.getTimeSlot() != null) {
            dto.setTimeSlot(new TimeSlotResponseDTO(
                    item.getTimeSlot().getStartTime(),
                    item.getTimeSlot().getEndTime()
            ));
        }

        // Type-specific fields
        if (item instanceof Task) {
            Task task = (Task) item;
            dto.setParentBigTaskId(task.getParentBigTaskId());
            dto.setEstimatedHours(task.getEstimatedHours());
            dto.setActualHours(task.getActualHours());
            dto.setDueDate(task.getDueDate());
            dto.setCompletionPercentage(task.getCompletionPercentage());

            // Convert subtasks
            List<SubtaskDTO> subtaskDTOs = task.getSubtasks().stream()
                    .map(subtask -> new SubtaskDTO(
                            subtask.getId(),
                            subtask.getName(),
                            subtask.getDescription(),
                            subtask.getIsComplete(),
                            subtask.getCompletedAt()
                    ))
                    .collect(Collectors.toList());
            dto.setSubtasks(subtaskDTOs);

        } else if (item instanceof Routine) {
            Routine routine = (Routine) item;

            // Convert recurring pattern
            if (routine.getPattern() != null) {
                List<String> daysOfWeek = routine.getPattern().getDaysOfWeek().stream()
                        .map(DayOfWeek::name)
                        .collect(Collectors.toList());
                dto.setPattern(new RecurringPatternResponseDTO(daysOfWeek));
            }

            dto.setExceptions(routine.getExceptions());

        } else if (item instanceof Event) {
            // Event has no additional fields currently
            // If you add location/attendees to Event entity later, map them here
        }

        return dto;
    }

}