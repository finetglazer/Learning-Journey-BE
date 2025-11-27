package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.client.ProjectServiceClient;
import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.payload.request.BatchScheduleRequest;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.TimeSlotDTO;
import com.graduation.schedulingservice.payload.request.UpdateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.repository.MonthPlanRepository;
import com.graduation.schedulingservice.repository.WeekPlanRepository;
import com.graduation.schedulingservice.service.CalendarItemService;
import com.graduation.schedulingservice.service.ConstraintValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final MonthPlanRepository monthPlanRepository;
    private final WeekPlanRepository weekPlanRepository;
    private final ProjectServiceClient projectServiceClient;

    @Override
    @Transactional
    public BaseResponse<?> createItem(Long userId, CreateCalendarItemRequest request) {
        try {
            // 1. Validate item type
            ItemType itemType;
            try {
                itemType = ItemType.valueOf(request.getType().trim().toUpperCase());
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

            // ===== NEW VALIDATION STEP =====
            // 3.5. Validate against existing scheduled routines
            if (request.getTimeSlot() != null) {
                // Fetch all *existing* scheduled routines that have a pattern
                List<Routine> scheduledRoutines = calendarItemRepository.findAllByUserId(userId).stream()
                        .filter(calendarItem -> calendarItem instanceof Routine)
                        .map(calendarItem -> (Routine) calendarItem)
                        .filter(routine -> routine.isScheduled() && routine.getPattern() != null &&
                                routine.getPattern().getDaysOfWeek() != null &&
                                !routine.getPattern().getDaysOfWeek().isEmpty())
                        .collect(Collectors.toList());

                if (!scheduledRoutines.isEmpty()) {
                    // Check for overlaps
                    Optional<String> routineOverlapError = createRequestFindRoutineOverlap(request, scheduledRoutines);
                    if (routineOverlapError.isPresent()) {
                        String errorMessage = routineOverlapError.get();
                        log.warn("Routine overlap detected for userId={}: {}", userId, errorMessage);
                        // Return a constraint violation response
                        return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, List.of(errorMessage));
                    }
                }
            }
            // ===== END OF NEW VALIDATION STEP =====

            // 4. Determine mode: Standalone (monthPlanId == null) or Month Plan Mode (monthPlanId != null)
            Long weekPlanId = null;

            if (request.getMonthPlanId() != null) {
                // ===== MONTH PLAN MODE =====

                // 4.1. Validate month plan exists and belongs to user
                Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(
                        request.getMonthPlanId(), userId);

                if (monthPlanOpt.isEmpty()) {
                    log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}",
                            request.getMonthPlanId(), userId);
                    return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
                }

                MonthPlan monthPlan = monthPlanOpt.get();

                // 4.2. Validate timeSlot is provided (required for scheduling)
                if (request.getTimeSlot() == null || request.getTimeSlot().getStartTime() == null) {
                    log.warn("TimeSlot is required when scheduling from month plan");
                    return new BaseResponse<>(0, "TimeSlot is required when scheduling from month plan", null);
                }

                // 4.3. Extract month from timeSlot
                LocalDate timeSlotDate = request.getTimeSlot().getStartTime().toLocalDate();
                int timeSlotYear = timeSlotDate.getYear();
                int timeSlotMonth = timeSlotDate.getMonthValue();

                // 4.4. Validate timeSlot month matches monthPlan month
                if (timeSlotYear != monthPlan.getYear() ||
                        timeSlotMonth != monthPlan.getMonth()) {

                    String timeSlotMonthStr = String.format("%d-%02d", timeSlotYear, timeSlotMonth);
                    String monthPlanMonthStr = String.format("%d-%02d",
                            monthPlan.getYear(), monthPlan.getMonth());

                    MonthMismatchErrorDetails errorDetails = new MonthMismatchErrorDetails(
                            timeSlotMonthStr,
                            monthPlanMonthStr,
                            monthPlan.getId()
                    );

                    MonthPlanErrorResponse errorResponse = new MonthPlanErrorResponse(
                            false,
                            "MONTH_MISMATCH",
                            String.format("Item's time slot (%s) does not match month plan (%s)",
                                    timeSlotMonthStr, monthPlanMonthStr),
                            errorDetails
                    );

                    log.warn("Month mismatch: timeSlot={}, monthPlan={}",
                            timeSlotMonthStr, monthPlanMonthStr);
                    return new BaseResponse<>(0, errorResponse.getMessage(), errorResponse);
                }

                // 4.5. TYPE-SPECIFIC VALIDATION
                if (itemType == ItemType.ROUTINE) {
                    // Validate routine name is in approved list
                    if (!monthPlan.getApprovedRoutineNames().contains(request.getName())) {
                        String requestedMonth = String.format("%d-%02d",
                                monthPlan.getYear(), monthPlan.getMonth());

                        RoutineErrorDetails errorDetails = new RoutineErrorDetails(
                                request.getName(),
                                "ROUTINE",
                                requestedMonth,
                                monthPlan.getId()
                        );

                        MonthPlanErrorResponse errorResponse = new MonthPlanErrorResponse(
                                false,
                                "INVALID_MONTH_PLAN_REFERENCE",
                                String.format("Routine '%s' is not approved in the %s month plan",
                                        request.getName(), requestedMonth),
                                errorDetails
                        );

                        log.warn("Routine not approved in month plan: name={}, monthPlanId={}",
                                request.getName(), monthPlan.getId());
                        return new BaseResponse<>(0, errorResponse.getMessage(), errorResponse);
                    }

                } else if (itemType == ItemType.TASK && request.getTaskDetails() != null
                        && request.getTaskDetails().getParentBigTaskId() != null) {

                    // Validate parentBigTaskId exists in this month plan
                    Long parentBigTaskId = request.getTaskDetails().getParentBigTaskId();
                    boolean bigTaskExists = monthPlan.getBigTasks().stream()
                            .anyMatch(bt -> bt.getId().equals(parentBigTaskId));

                    if (!bigTaskExists) {
                        String requestedMonth = String.format("%d-%02d",
                                monthPlan.getYear(), monthPlan.getMonth());

                        TaskErrorDetails errorDetails = new TaskErrorDetails(
                                parentBigTaskId,
                                requestedMonth,
                                monthPlan.getId()
                        );

                        MonthPlanErrorResponse errorResponse = new MonthPlanErrorResponse(
                                false,
                                "INVALID_MONTH_PLAN_REFERENCE",
                                String.format("Parent big task ID %d not found in %s month plan",
                                        parentBigTaskId, requestedMonth),
                                errorDetails
                        );

                        log.warn("Big task not found in month plan: bigTaskId={}, monthPlanId={}",
                                parentBigTaskId, monthPlan.getId());
                        return new BaseResponse<>(0, errorResponse.getMessage(), errorResponse);
                    }
                }
                // EVENT: No additional validation needed

                // 4.6. Determine weekPlanId from timeSlot
                Optional<WeekPlan> weekPlanOpt = weekPlanRepository.findByMonthPlanIdAndDateWithin(
                        monthPlan.getId(), timeSlotDate);

                if (weekPlanOpt.isPresent()) {
                    weekPlanId = weekPlanOpt.get().getId();
                } else {
                    log.warn("No week plan found for date: date={}, monthPlanId={}",
                            timeSlotDate, monthPlan.getId());
                    return new BaseResponse<>(0, "No week plan found for the specified date", null);
                }
            }
            // else: Standalone Mode - monthPlanId and weekPlanId remain null

            if (request.getPmTaskId() != null) {
                Optional<PM_TasKDTO> res = projectServiceClient.getProjectTaskById(request.getPmTaskId());
                if (res.isEmpty()) {
                    log.warn("No project task found for pmTaskId: {}",
                            request.getPmTaskId());
                    return new BaseResponse<>(0, "No project task found", null);
                }
            }

            // 5. Validate constraints (overlapping, sleep hours, daily limits)
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

            // 6. Create calendar item based on type
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
                case PROJECT_WORK:
                    calendarItem = createProjectTask(userId, request);
                    break;
                default:
                    return new BaseResponse<>(0, Constant.MSG_INVALID_ITEM_TYPE, null);
            }

            // 7. Set month plan and week plan IDs
            calendarItem.setMonthPlanId(request.getMonthPlanId());
            calendarItem.setWeekPlanId(weekPlanId);

            // 8. Save to database
            CalendarItem savedItem = calendarItemRepository.save(calendarItem);

            // 9. Return success response
            CreateItemResponse response = new CreateItemResponse(
                    true,
                    savedItem.getId(),
                    Constant.MSG_ITEM_CREATED_SUCCESS
            );

            log.info("Calendar item created successfully: userId={}, itemId={}, type={}, mode={}",
                    userId, savedItem.getId(), itemType,
                    request.getMonthPlanId() != null ? "MONTH_PLAN" : "STANDALONE");

            return new BaseResponse<>(1, Constant.MSG_ITEM_CREATED_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_ITEM_CREATION_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEM_CREATION_FAILED, null);
        }
    }

    /**
     * Checks if a new item request overlaps with any existing scheduled routines for CreateCalendarItemRequest.
     *
     * @param request           The new item being created.
     * @param scheduledRoutines A list of existing, scheduled routines for the user.
     * @return An Optional containing an error message if an overlap is found.
     */
    private Optional<String> createRequestFindRoutineOverlap(CreateCalendarItemRequest request, List<Routine> scheduledRoutines) {
        ItemType newItemType = ItemType.valueOf(request.getType().toUpperCase());
        TimeSlotDTO newItemTimeSlot = request.getTimeSlot();

        // If the new item isn't scheduled, it can't overlap.
        if (newItemTimeSlot == null || newItemTimeSlot.getStartTime() == null) {
            return Optional.empty();
        }

        // Formatter for user-friendly time (e.g., 6:30 PM)
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        if (newItemType == ItemType.TASK || newItemType == ItemType.EVENT) {
            // --- Case 1: New item is a TASK or EVENT ---
            // Check its specific date, day, and time against all active routines.

            LocalDateTime newItemStartTime = newItemTimeSlot.getStartTime();
            LocalDateTime newItemEndTime = newItemTimeSlot.getEndTime();

            LocalDate newItemDate = newItemStartTime.toLocalDate();
            DayOfWeek newDayOfWeek = newItemDate.getDayOfWeek();
            LocalTime newItemTimeStart = newItemStartTime.toLocalTime();
            LocalTime newItemTimeEnd = newItemEndTime.toLocalTime();


            for (Routine existingRoutine : scheduledRoutines) {
                // 1. Get existing routine's active date range
                // FIX: Re-introduced date check
                LocalDate routineActiveStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                // Assuming routine is active for the whole month it was created in
                LocalDate routineActiveEndDate = routineActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());

                // 2. Check if the new item's date is within the routine's active range
                boolean dateRangeOverlaps = !newItemDate.isBefore(routineActiveStartDate) &&
                        !newItemDate.isAfter(routineActiveEndDate);

                if (!dateRangeOverlaps) {
                    continue; // This routine isn't active on this day, skip
                }
                // --- End of FIX ---

                // 3. Check if the routine runs on the same day of the week
                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                boolean dayOfWeekOverlaps = existingDays.contains(newDayOfWeek);

                if (!dayOfWeekOverlaps) {
                    continue; // This routine doesn't run on this day of the week, skip
                }

                // 4. Check if the times overlap
                LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                if (timesOverlap(newItemTimeStart, newItemTimeEnd, existingStartTime, existingEndTime)) {
                    // Capitalize day of week (e.g., "Monday")
                    String friendlyDayOfWeek = newDayOfWeek.toString().charAt(0) + newDayOfWeek.toString().substring(1).toLowerCase();

                    return Optional.of(String.format(
                            "New %s overlaps with existing routine '%s' on %s (%s - %s)",
                            newItemType.name().toLowerCase(),
                            existingRoutine.getName(),
                            friendlyDayOfWeek,
                            existingStartTime.format(timeFormatter), // "6:30 PM"
                            existingEndTime.format(timeFormatter)    // "7:30 PM"
                    ));
                }
            }

        } else if (newItemType == ItemType.ROUTINE) {
            // --- Case 2: New item is a ROUTINE ---
            // Check if its pattern (days and time) overlaps with any other routine.

            if (request.getRoutineDetails() == null || request.getRoutineDetails().getPattern() == null ||
                    request.getRoutineDetails().getPattern().getDaysOfWeek() == null) {
                return Optional.empty(); // Not a recurring routine, no overlap to check
            }

            // 1. Get new routine's details
            // FIX: Re-introduced date check
            LocalDate newRoutineActiveStartDate = newItemTimeSlot.getStartTime().toLocalDate();
            LocalDate newRoutineActiveEndDate = newRoutineActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());
            // --- End of FIX ---

            LocalTime newRoutineTimeStart = newItemTimeSlot.getStartTime().toLocalTime();
            LocalTime newRoutineTimeEnd = newItemTimeSlot.getEndTime().toLocalTime();
            List<DayOfWeek> newDays = request.getRoutineDetails().getPattern().getDaysOfWeek().stream()
                    .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                    .collect(Collectors.toList());

            for (Routine existingRoutine : scheduledRoutines) {
                // 1. Get existing routine's details
                // FIX: Re-introduced date check
                LocalDate existingActiveStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                LocalDate existingActiveEndDate = existingActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());

                // 2. Check if the active *date ranges* overlap (e.g., both active in the same month)
                boolean dateRangeOverlaps = (newRoutineActiveStartDate.isBefore(existingActiveEndDate) || newRoutineActiveStartDate.isEqual(existingActiveEndDate)) &&
                        (existingActiveStartDate.isBefore(newRoutineActiveEndDate) || existingActiveStartDate.isEqual(newRoutineActiveEndDate));

                if (!dateRangeOverlaps) {
                    continue; // Routines are active in different months, skip
                }
                // --- End of FIX ---

                // 3. Check if they run on any of the same *days of the week*
                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                boolean dayOfWeekOverlaps = newDays.stream().anyMatch(existingDays::contains);

                if (!dayOfWeekOverlaps) {
                    continue; // No common days, skip
                }

                // 4. Check if the *times* overlap
                LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                if (timesOverlap(newRoutineTimeStart, newRoutineTimeEnd, existingStartTime, existingEndTime)) {
                    // Find one of the overlapping days to show in the message
                    String overlappingDay = newDays.stream()
                            .filter(existingDays::contains)
                            .findFirst()
                            .map(day -> day.toString().charAt(0) + day.toString().substring(1).toLowerCase())
                            .orElse("a recurring day"); // e.g., "Monday"

                    return Optional.of(String.format(
                            "New routine pattern overlaps with existing routine '%s' on %s (%s - %s)",
                            existingRoutine.getName(),
                            overlappingDay,
                            existingStartTime.format(timeFormatter),
                            existingEndTime.format(timeFormatter)
                    ));
                }
            }
        }

        return Optional.empty(); // No overlaps found
    }

    /**
     * Checks if a new item request overlaps with any existing scheduled routines for UpdateCalendarItemRequest.
     *
     * @param request           The item being updated.
     * @param scheduledRoutines A list of existing, scheduled routines for the user.
     * @return An Optional containing an error message if an overlap is found.
     */
    private Optional<String> updateRequestFindRoutineOverlap(UpdateCalendarItemRequest request, Long updateItemId, List<Routine> scheduledRoutines) {
        List<Routine> routinesToCompare = scheduledRoutines.stream()
                .filter(routine -> routine.isScheduled() && !routine.getId().equals(updateItemId))
                .collect(Collectors.toList());

        // If no *other* routines exist, we can't overlap
        if (routinesToCompare.isEmpty()) {
            return Optional.empty();
        }

        TimeSlotDTO newItemTimeSlot = request.getTimeSlot();

        // If the new item isn't scheduled, it can't overlap.
        if (newItemTimeSlot == null || newItemTimeSlot.getStartTime() == null) {
            return Optional.empty();
        }

        ItemType itemType = getUpdateRequestItemType(request);

        if (itemType == ItemType.TASK || itemType == ItemType.EVENT) {
            // --- Case 1: Item is a TASK or EVENT ---
            // Check its specific date, day, and time against all active routines.

            LocalDateTime newItemStartTime = newItemTimeSlot.getStartTime();
            LocalDateTime newItemEndTime = newItemTimeSlot.getEndTime();

            LocalDate newItemDate = newItemStartTime.toLocalDate();
            DayOfWeek newDayOfWeek = newItemDate.getDayOfWeek();
            LocalTime newItemTimeStart = newItemStartTime.toLocalTime();
            LocalTime newItemTimeEnd = newItemEndTime.toLocalTime();


            for (Routine existingRoutine : routinesToCompare) {
                // 1. Get existing routine's active date range
                LocalDate routineActiveStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                LocalDate routineActiveEndDate = routineActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());

                // 2. Check if the new item's date is within the routine's active range
                boolean dateRangeOverlaps = !newItemDate.isBefore(routineActiveStartDate) &&
                        !newItemDate.isAfter(routineActiveEndDate);

                if (!dateRangeOverlaps) {
                    continue; // This routine isn't active on this day, skip
                }

                // 3. Check if the routine runs on the same day of the week
                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                boolean dayOfWeekOverlaps = existingDays.contains(newDayOfWeek);

                if (!dayOfWeekOverlaps) {
                    continue; // This routine doesn't run on this day of the week, skip
                }

                // 4. Check if the times overlap
                LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                if (timesOverlap(newItemTimeStart, newItemTimeEnd, existingStartTime, existingEndTime)) {
                    return Optional.of(String.format(
                            "New %s overlaps with existing routine '%s' on %s (%s - %s)",
                            itemType.name().toLowerCase(),
                            existingRoutine.getName(),
                            newDayOfWeek,
                            existingStartTime,
                            existingEndTime
                    ));
                }
            }

        } else if (itemType == ItemType.ROUTINE) {
            // --- Case 2: Item is a ROUTINE ---
            // Check if its pattern, time, and active date range overlap with any other routine.

            // Note: We use request.getRoutineDetails() because the user might be updating the pattern
            if (request.getRoutineDetails() == null || request.getRoutineDetails().getPattern() == null ||
                    request.getRoutineDetails().getPattern().getDaysOfWeek() == null) {
                return Optional.empty(); // Not a recurring routine, no overlap to check
            }

            // 1. Get new routine's details
            LocalDate newRoutineActiveStartDate = newItemTimeSlot.getStartTime().toLocalDate();
            LocalDate newRoutineActiveEndDate = newRoutineActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());
            LocalTime newRoutineTimeStart = newItemTimeSlot.getStartTime().toLocalTime();
            LocalTime newRoutineTimeEnd = newItemTimeSlot.getEndTime().toLocalTime();
            List<DayOfWeek> newDays = request.getRoutineDetails().getPattern().getDaysOfWeek().stream()
                    .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                    .collect(Collectors.toList());

            for (Routine existingRoutine : routinesToCompare) {
                // 1. Get existing routine's details
                LocalDate existingActiveStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                LocalDate existingActiveEndDate = existingActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());

                // 2. Check if the active *date ranges* overlap (e.g., both active in the same month)
                boolean dateRangeOverlaps = (newRoutineActiveStartDate.isBefore(existingActiveEndDate) || newRoutineActiveStartDate.isEqual(existingActiveEndDate)) &&
                        (existingActiveStartDate.isBefore(newRoutineActiveEndDate) || existingActiveStartDate.isEqual(newRoutineActiveEndDate));

                if (!dateRangeOverlaps) {
                    continue; // Routines are active in different months, skip
                }

                // 3. Check if they run on any of the same *days of the week*
                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                boolean dayOfWeekOverlaps = newDays.stream().anyMatch(existingDays::contains);

                if (!dayOfWeekOverlaps) {
                    continue; // No common days, skip
                }

                // 4. Check if the *times* overlap
                LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                if (timesOverlap(newRoutineTimeStart, newRoutineTimeEnd, existingStartTime, existingEndTime)) {
                    return Optional.of(String.format(
                            "New routine pattern overlaps with existing routine '%s' (%s - %s)",
                            existingRoutine.getName(),
                            existingStartTime,
                            existingEndTime
                    ));
                }
            }
        }

        return Optional.empty(); // No overlaps found
    }

    /**
     * Helper method to check if two LocalTime ranges overlap.
     * (start1 < end2) AND (start2 < end1)
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private ItemType getUpdateRequestItemType(UpdateCalendarItemRequest request) {
        if (request.getTaskDetails() != null) {
            return ItemType.TASK;
        }
        if (request.getEventDetails() != null) {
            return ItemType.EVENT;
        }
        if (request.getRoutineDetails() != null) {
            return ItemType.ROUTINE;
        }
        return ItemType.TASK;
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

            if (request.getTaskDetails().getParentBigTaskId() != null) {
                task.setParentBigTaskId(request.getTaskDetails().getParentBigTaskId());
            }
        }

        return task;
    }

    /**
     * Create a Project task item
     */
    private ProjectTask createProjectTask(Long userId, CreateCalendarItemRequest request) {
        ProjectTask projectTask = new ProjectTask();
        setCommonFields(projectTask, userId, request);
        projectTask.setPmTaskId(request.getPmTaskId());
        return projectTask;
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

                // ===== NEW VALIDATION STEP =====
                // 3.5. Validate against existing scheduled routines
                if (request.getTimeSlot() != null) {
                    // Fetch all *existing* scheduled routines that have a pattern
                    List<Routine> scheduledRoutines = calendarItemRepository.findAllByUserId(userId).stream()
                            .filter(calendarItem -> calendarItem instanceof Routine)
                            .map(calendarItem -> (Routine) calendarItem)
                            .filter(routine -> routine.isScheduled() && routine.getPattern() != null &&
                                    routine.getPattern().getDaysOfWeek() != null &&
                                    !routine.getPattern().getDaysOfWeek().isEmpty())
                            .collect(Collectors.toList());

                    if (!scheduledRoutines.isEmpty()) {
                        // Check for overlaps
                        Optional<String> routineOverlapError = updateRequestFindRoutineOverlap(request, itemId, scheduledRoutines);
                        if (routineOverlapError.isPresent()) {
                            String errorMessage = routineOverlapError.get();
                            log.warn("Routine overlap detected for userId={}: {}", userId, errorMessage);
                            // Return a constraint violation response
                            return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, List.of(errorMessage));
                        }
                    }
                }
                // ===== END OF NEW VALIDATION STEP =====

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
     * Validate constraints for update operation.
     * Excludes the current item from overlap checking.
     * This method performs a custom overlap check to avoid self-collision.
     *
     * @param userId    The user's ID.
     * @param itemId    The ID of the item being updated.
     * @param startTime The new start time.
     * @param endTime   The new end time.
     * @param itemType  The item's type.
     * @return A list of violation messages.
     */
    private List<String> validateConstraintsForUpdate(Long userId, Long itemId,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      ItemType itemType) {

        // --- FIX: ---
        // 1. Call a method that validates *only* non-overlap constraints (like sleep, daily limits).
        // We assume this method is named 'validateBaseConstraints'.
        // We NO LONGER call the all-in-one 'validateConstraints' method here, as it
        // would incorrectly find an overlap with the item itself.
        List<String> violations = constraintValidationService.validateBaseConstraints(
                userId, startTime, endTime, itemType);

        // Formatter for user-friendly times
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");

        // --- 2. Check for Overlaps with FIXED Items (Tasks/Events) ---

        // Find all fixed items that overlap using the JPA query
        List<CalendarItem> overlappingFixedItems = calendarItemRepository
                .findOverlappingItems(userId, startTime, endTime);

        // Filter out the current item (self-overlap)
        List<CalendarItem> actualFixedOverlaps = overlappingFixedItems.stream()
                .filter(item -> !item.getId().equals(itemId)) // Exclude self
                .collect(Collectors.toList());

        if (!actualFixedOverlaps.isEmpty()) {
            // Create a user-friendly list of overlapping items
            String itemNames = actualFixedOverlaps.stream()
                    .map(item -> {
                        LocalDateTime itemStart = item.getTimeSlot().getStartTime();
                        LocalDateTime itemEnd = item.getTimeSlot().getEndTime();
                        String timeString;
                        if (itemStart.toLocalDate().isEqual(itemEnd.toLocalDate())) {
                            timeString = String.format("%s, %s - %s",
                                    itemStart.format(DateTimeFormatter.ofPattern("MMM d")),
                                    itemStart.format(timeFormatter),
                                    itemEnd.format(timeFormatter)
                            );
                        } else {
                            timeString = String.format("%s - %s",
                                    itemStart.format(fullFormatter),
                                    itemEnd.format(fullFormatter)
                            );
                        }
                        return String.format("'%s' (%s)", item.getName(), timeString);
                    })
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("existing items");

            violations.add(String.format(
                    "Time slot overlaps with existing calendar items: %s", itemNames));
        }

        // --- 3. Check for Overlaps with RECURRING Items (Routines) ---

        // This check is only needed if the item being updated is a TASK or EVENT.
        // If the item being updated is a ROUTINE, its own dedicated
        // 'updateRequestFindRoutineOverlap' method should be called instead.

        if (itemType == ItemType.TASK || itemType == ItemType.EVENT) {

            // Get all existing routines
            List<Routine> scheduledRoutines = calendarItemRepository.findAllByUserId(userId).stream()
                    .filter(item -> item instanceof Routine)
                    .map(item -> (Routine) item)
                    .filter(Routine::isScheduled) // Has a pattern and timeslot
                    .collect(Collectors.toList());

            // Note: No need to filter by 'itemId' here, because a TASK or EVENT
            // can never have the same ID as a ROUTINE.

            if (!scheduledRoutines.isEmpty()) {
                DayOfWeek newDayOfWeek = startTime.toLocalDate().getDayOfWeek();
                LocalTime newStartTime = startTime.toLocalTime();
                LocalTime newEndTime = endTime.toLocalTime();

                for (Routine existingRoutine : scheduledRoutines) {
                    // 3a. Check if the routine runs on the same day of the week
                    List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                    if (existingDays.contains(newDayOfWeek)) {

                        // 3b. Check if the times overlap
                        LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                        LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                        // Assuming you have a 'timesOverlap' helper method
                        if (timesOverlap(newStartTime, newEndTime, existingStartTime, existingEndTime)) {
                            String friendlyDayOfWeek = newDayOfWeek.toString().substring(0, 1) + newDayOfWeek.toString().substring(1).toLowerCase();
                            violations.add(String.format(
                                    "Time slot overlaps with your recurring routine '%s' on %s (%s - %s)",
                                    existingRoutine.getName(),
                                    friendlyDayOfWeek,
                                    existingStartTime.format(timeFormatter),
                                    existingEndTime.format(timeFormatter)
                            ));
                        }
                    }
                }
            }
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

            // 4. Initialize the final list
            List<ScheduledItemDTO> finalItemList = new ArrayList<>();

            // 5. --- FIX: FETCH NON-RECURRING ITEMS (TASKS/EVENTS) ---
            // This gets all Tasks, Events, and non-recurring Routines
            List<CalendarItem> nonRecurringItems = calendarItemRepository.findScheduledItemsExcludingRoutinesByDateRange(
                    userId, calendarIds, dateRange.getStart(), dateRange.getEnd());

            // 6. Add Task, Event items to the list
            nonRecurringItems.stream()
                    .map(this::mapToScheduledItemDTO)
                    .forEach(finalItemList::add);

            // 7. Fetch data from repository (Recurring Routines)
            // This gets all potentially active recurring routines
            List<Routine> recurringRoutines = calendarItemRepository.findAllRecurringRoutinesStartedBefore(
                    userId, calendarIds, dateRange.getEnd());

            // 8. Filter routines whose active month overlaps the date range
            LocalDate rangeStartDate = dateRange.getStart().toLocalDate();
            LocalDate rangeEndDate = dateRange.getEnd().toLocalDate();

            List<Routine> activeRoutines = recurringRoutines.stream()
                    .filter(routine -> {
                        LocalDate routineActiveStartDate = routine.getTimeSlot().getStartTime().toLocalDate();
                        LocalDate routineActiveEndDate = routineActiveStartDate.with(TemporalAdjusters.lastDayOfMonth());

                        // Check for overlap: (StartA <= EndB) and (StartB <= EndA)
                        boolean overlaps = (routineActiveStartDate.isBefore(rangeEndDate) || routineActiveStartDate.isEqual(rangeEndDate)) &&
                                (rangeStartDate.isBefore(routineActiveEndDate) || rangeStartDate.isEqual(routineActiveEndDate));

                        return overlaps;
                    })
                    .collect(Collectors.toList());

            // 9. Add the *original* Routine objects (not expanded)
            activeRoutines.stream()
                    .map(this::mapToScheduledItemDTO)
                    .forEach(finalItemList::add);

            // 10. Sort the final combined list
            finalItemList.sort(Comparator.comparing(dto -> dto.getTimeSlot().getStartTime()));

            // 11. Build and return response
            ItemsByDateRangeResponse response = new ItemsByDateRangeResponse(finalItemList, "startTime", dateRange);
            return new BaseResponse<>(1, Constant.MSG_ITEMS_RETRIEVED_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_ITEMS_BY_DATE_RANGE_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_ITEMS_RETRIEVAL_FAILED, null);
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
        if (item instanceof Routine) {
            Routine routine = (Routine) item;
            dto.setPattern(routine.getPattern());
        }
        return dto;
    }

    private UnscheduledRoutineDTO mapToUnscheduledRoutineDTO(Routine routine) {
        UnscheduledRoutineDTO dto = new UnscheduledRoutineDTO();
        dto.setId(routine.getId());
        dto.setName(routine.getName());
        dto.setSource(routine.getMonthPlanId() != null ? "MONTH_PLAN" : "CALENDAR");
        dto.setCanUsePreviousTiming(true); // Placeholder logic as per spec
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