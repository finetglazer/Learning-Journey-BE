package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.*;
import com.graduation.schedulingservice.payload.request.BatchScheduleRequest;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
                log.warn(Constant.LOG_INVALID_ITEM_TYPE, request.getType());
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
                log.warn(Constant.LOG_CALENDAR_UNAUTHORIZED,
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
                    log.warn(Constant.LOG_CONSTRAINT_VIOLATIONS, userId, violations);
                    return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, violations);
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
            return new BaseResponse<>(0, Constant.MSG_ITEM_CREATION_FAILED, null);
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
                                log.warn(Constant.LOG_INVALID_DAY_OF_WEEK, day);
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
            log.info(Constant.LOG_TIMEZONE_CONVERT_START, userId, oldTimezone, newTimezone);

            ZoneId oldZone;
            ZoneId newZone;
            try {
                oldZone = ZoneId.of(oldTimezone);
                newZone = ZoneId.of(newTimezone);
            } catch (DateTimeException e) {
                log.warn(Constant.LOG_INVALID_TIMEZONE, oldTimezone, newTimezone);
                return new BaseResponse<>(0, Constant.MSG_INVALID_TIMEZONE_FORMAT, null);
            }

            List<CalendarItem> items = calendarItemRepository.findAllByUserId(userId);

            if (items.isEmpty()) {
                log.info(Constant.LOG_NO_ITEMS_FOR_USER, userId);
                return new BaseResponse<>(1, Constant.MSG_NO_ITEMS_TO_CONVERT, null);
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

            log.info(Constant.LOG_CONVERSION_SUCCESS, convertedCount, userId);
            return new BaseResponse<>(1,
                    String.format(Constant.MSG_CONVERSION_SUCCESS, convertedCount),
                    null);

        } catch (Exception e) {
            log.error(Constant.LOG_CONVERSION_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_CONVERSION_FAILED, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getItemById(Long userId, Long itemId) {
        try {
            log.info(Constant.LOG_FETCHING_ITEM, userId, itemId);

            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn(Constant.LOG_ITEM_NOT_FOUND, itemId);
                return new BaseResponse<>(0, Constant.MSG_ITEM_NOT_FOUND, null);
            }

            CalendarItem item = itemOpt.get();

            // Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn(Constant.LOG_UNAUTHORIZED_ACCESS,
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, Constant.MSG_UNAUTHORIZED_ACCESS, null);
            }

            // Convert to DTO
            CalendarItemResponseDTO responseDTO = convertToResponseDTO(item);

            log.info(Constant.LOG_ITEM_FETCHED_SUCCESS, itemId);
            return new BaseResponse<>(1, Constant.MSG_ITEM_FETCH_SUCCESS, responseDTO);

        } catch (Exception e) {
            log.error(Constant.LOG_FETCHING_ITEM_FAILED, itemId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEM_FETCH_FAILED, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateItem(Long userId, Long itemId, UpdateCalendarItemRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_ITEM, userId, itemId);

            // 1. Find existing item
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn(Constant.LOG_ITEM_NOT_FOUND, itemId);
                return new BaseResponse<>(0, Constant.MSG_ITEM_NOT_FOUND, null);
            }

            CalendarItem item = itemOpt.get();

            // 2. Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn(Constant.LOG_UNAUTHORIZED_UPDATE,
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, Constant.MSG_UNAUTHORIZED_ACCESS, null);
            }

            // 2.1 Validate name of item not null or empty
            if (request.getName() != null && request.getName().trim().isEmpty()) {
                log.warn(Constant.LOG_INVALID_NAME_UPDATE, itemId);
                return new BaseResponse<>(0, Constant.MSG_ITEM_NAME_EMPTY, null);
            }

            // 3. Validate new time slot if provided
            if (request.getTimeSlot() != null) {
                TimeSlotDTO timeSlotDTO = request.getTimeSlot();

                // Validate start < end
                if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                        timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                    log.warn(Constant.LOG_INVALID_TIME_SLOT,
                            timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_SLOT, null);
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
                    log.warn(Constant.LOG_CONSTRAINT_VIOLATIONS, userId, violations);
                    return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, violations);
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
                    log.warn(Constant.LOG_INVALID_STATUS_UPDATE, request.getStatus());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_STATUS_VALUE, null);
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
                                        log.warn(Constant.LOG_INVALID_DAY_OF_WEEK, day);
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

            log.info(Constant.LOG_ITEM_UPDATED_SUCCESS, itemId);
            return new BaseResponse<>(1, Constant.MSG_ITEM_UPDATE_SUCCESS, updatedItem.getId());

        } catch (Exception e) {
            log.error(Constant.LOG_UPDATE_ITEM_FAILED, itemId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEM_UPDATE_FAILED, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteItem(Long userId, Long itemId) {
        try {
            log.info(Constant.LOG_DELETING_ITEM, userId, itemId);

            // 1. Find existing item
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn(Constant.LOG_ITEM_NOT_FOUND, itemId);
                return new BaseResponse<>(0, Constant.MSG_ITEM_NOT_FOUND, null);
            }

            CalendarItem item = itemOpt.get();

            // 2. Verify ownership
            if (!item.getUserId().equals(userId)) {
                log.warn(Constant.LOG_UNAUTHORIZED_DELETE,
                        userId, itemId, item.getUserId());
                return new BaseResponse<>(0, Constant.MSG_UNAUTHORIZED_ACCESS, null);
            }

            // 3. Delete the item
            calendarItemRepository.delete(item);

            log.info(Constant.LOG_ITEM_DELETED_SUCCESS, itemId);
            return new BaseResponse<>(1, Constant.MSG_ITEM_DELETE_SUCCESS, null);

        } catch (Exception e) {
            log.error(Constant.LOG_DELETE_ITEM_FAILED, itemId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEM_DELETE_FAILED, null);
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
            violations.removeIf(v -> v.contains(Constant.VALIDATION_OVERLAP_MESSAGE));
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

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getItemsByDateRange(Long userId, String view, String date, List<Long> calendarIds) {
        try {
            // 1. Validate and parse date
            LocalDate referenceDate;
            try {
                referenceDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                log.warn(Constant.LOG_INVALID_DATE_FORMAT, date);
                return new BaseResponse<>(0, Constant.MSG_INVALID_DATE_FORMAT, null);
            }

            // 2. 5-year window validation
            if (referenceDate.isAfter(LocalDate.now().plusYears(5)) || referenceDate.isBefore(LocalDate.now().minusYears(5))) {
                log.warn(Constant.LOG_DATE_OUTSIDE_WINDOW, referenceDate);
                return new BaseResponse<>(0, Constant.MSG_DATE_OUTSIDE_WINDOW, null);
            }

            // 3. Calculate date range based on view
            DateRangeDTO dateRange = calculateDateRange(view, referenceDate);
            if (dateRange == null) {
                log.warn(Constant.LOG_INVALID_VIEW_TYPE, view);
                return new BaseResponse<>(0, Constant.MSG_INVALID_VIEW_TYPE, null);
            }

            // 4. Fetch data from repository
            List<CalendarItem> items = calendarItemRepository.findScheduledItemsByDateRange(
                    userId, calendarIds, dateRange.getStart(), dateRange.getEnd());

            // 5. Map to DTOs
            List<ScheduledItemDTO> itemDTOs = items.stream()
                    .map(this::mapToScheduledItemDTO)
                    .collect(Collectors.toList());

            // 6. Build and return response
            ItemsByDateRangeResponse response = new ItemsByDateRangeResponse(itemDTOs, "startTime", dateRange);
            return new BaseResponse<>(1, Constant.MSG_ITEMS_RETRIEVED_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_ITEMS_BY_DATE_RANGE_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEMS_RETRIEVAL_FAILED, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getUnscheduledItems(Long userId, Long weekPlanId) {
        try {
            List<CalendarItem> items;
            // 1. Fetch data based on whether weekPlanId is provided
            if (weekPlanId != null) {
                items = calendarItemRepository.findUnscheduledByUserIdAndWeekPlanId(userId, weekPlanId);
            } else {
                items = calendarItemRepository.findUnscheduledByUserId(userId);
            }

            // 2. Separate items into routines and tasks
            List<UnscheduledRoutineDTO> routines = new ArrayList<>();
            List<UnscheduledTaskDTO> tasks = new ArrayList<>();

            for (CalendarItem item : items) {
                if (item instanceof Routine) {
                    routines.add(mapToUnscheduledRoutineDTO((Routine) item));
                } else if (item instanceof Task) {
                    tasks.add(mapToUnscheduledTaskDTO((Task) item));
                }
            }

            // 3. Build and return response
            UnscheduledItemsResponse response = new UnscheduledItemsResponse(routines, tasks);
            return new BaseResponse<>(1, Constant.MSG_UNSCHEDULED_ITEMS_RETRIEVED_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_UNSCHEDULED_ITEMS_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_UNSCHEDULED_ITEMS_RETRIEVAL_FAILED, null);
        }
    }

    // Private helper methods for the new logic
    private DateRangeDTO calculateDateRange(String view, LocalDate date) {
        LocalDateTime start, end;
        switch (view.toUpperCase()) {
            case "DAY":
                start = date.atStartOfDay();
                end = date.atTime(LocalTime.MAX);
                break;
            case "WEEK":
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
                break;
            case "MONTH":
                start = date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                end = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                break;
            case "YEAR":
                start = date.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                end = date.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
                break;
            default:
                return null;
        }
        return new DateRangeDTO(start, end);
    }

    private ScheduledItemDTO mapToScheduledItemDTO(CalendarItem item) {
        ScheduledItemDTO dto = new ScheduledItemDTO();
        dto.setId(item.getId());
        dto.setType(item.getType().name());
        dto.setName(item.getName());
        dto.setColor(item.getColor());
        dto.setStatus(item.getStatus().name());
        if (item.getTimeSlot() != null) {
            dto.setTimeSlot(new TimeSlotResponseDTO(
                    item.getTimeSlot().getStartTime(),
                    item.getTimeSlot().getEndTime()
            ));
        }
        return dto;
    }

    private UnscheduledRoutineDTO mapToUnscheduledRoutineDTO(Routine routine) {
        UnscheduledRoutineDTO dto = new UnscheduledRoutineDTO();
        dto.setId(routine.getId());
        dto.setName(routine.getName());
        dto.setSource(routine.getMonthPlanId() != null ? "MONTH_PLAN" : "CALENDAR");
        dto.setCanUseFormerTiming(true); // Placeholder logic as per spec
        return dto;
    }

    private UnscheduledTaskDTO mapToUnscheduledTaskDTO(Task task) {
        UnscheduledTaskDTO dto = new UnscheduledTaskDTO();
        dto.setSource(task.getMonthPlanId() != null ? "MONTH_PLAN" : "CALENDAR");

        if (task.getParentBigTaskId() == null) {
            dto.setBigTaskId(task.getId());
            dto.setBigTaskName(task.getName());
        } else {
            dto.setTaskId(task.getId());
            dto.setName(task.getName());
        }

        if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            dto.setSubtasks(task.getSubtasks().stream()
                    .map(subtask -> new SubtaskDTO(
                            subtask.getId(),
                            subtask.getName(),
                            subtask.getDescription(),
                            subtask.getIsComplete(),
                            subtask.getCompletedAt()
                    ))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    @Override
    @Transactional
    public BaseResponse<?> batchScheduleItems(Long userId, BatchScheduleRequest request) {
        int scheduledCount = 0;
        List<String> constraintViolations = new ArrayList<>();
        List<CalendarItem> itemsToUpdate = new ArrayList<>();

        for (BatchScheduleRequest.ItemToSchedule itemToSchedule : request.getItems()) {
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(itemToSchedule.getItemId());

            if (itemOpt.isEmpty()) {
                constraintViolations.add("Item with ID " + itemToSchedule.getItemId() + " not found.");
                continue;
            }

            CalendarItem item = itemOpt.get();

            if (!item.getUserId().equals(userId)) {
                constraintViolations.add("Unauthorized access to item with ID " + itemToSchedule.getItemId() + ".");
                continue;
            }

            TimeSlotDTO timeSlotDTO = itemToSchedule.getTimeSlot();
            if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                    timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                constraintViolations.add("Invalid time slot for item with ID " + itemToSchedule.getItemId() + ".");
                continue;
            }

            List<String> violations = validateConstraintsForUpdate(
                    userId,
                    itemToSchedule.getItemId(),
                    timeSlotDTO.getStartTime(),
                    timeSlotDTO.getEndTime(),
                    item.getType()
            );

            if (!violations.isEmpty()) {
                constraintViolations.addAll(violations);
                continue;
            }

            item.setTimeSlot(new TimeSlot(timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime()));
            itemsToUpdate.add(item);
            scheduledCount++;
        }

        if (!itemsToUpdate.isEmpty()) {
            calendarItemRepository.saveAll(itemsToUpdate);
        }

        return new BaseResponse<>(1, "Batch schedule operation completed.", new BatchScheduleResponse(true, scheduledCount, constraintViolations));
    }

}