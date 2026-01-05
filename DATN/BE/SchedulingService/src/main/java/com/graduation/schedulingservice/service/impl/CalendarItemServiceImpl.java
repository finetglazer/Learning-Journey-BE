package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.client.ProjectServiceClient;
import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.payload.request.BatchScheduleRequest;
import com.graduation.schedulingservice.payload.request.CreateCalendarItemRequest;
import com.graduation.schedulingservice.payload.request.TimeSlotDTO;
import com.graduation.schedulingservice.payload.request.RoutineDetailsDTO;
import com.graduation.schedulingservice.payload.request.UpdateCalendarItemRequest;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.repository.BigTaskRepository;
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
    private final BigTaskRepository bigTaskRepository;
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

            // find calendar by UserId
            List<com.graduation.schedulingservice.model.Calendar> userCalendars = calendarRepository
                    .findByUserId(userId);
            if (userCalendars.isEmpty()) {
                log.warn("No calendars found for user: userId={}", userId);
                return new BaseResponse<>(0, "No calendar found for this user. Cannot create tasks.", null);
            }
            Calendar defaultCalendar = userCalendars.get(0);

            // 3. Validate calendar ownership
            if (!calendarRepository.existsByIdAndUserId(defaultCalendar.getId(), userId)) {
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

            // 4. Determine mode: Standalone (monthPlanId == null) or Month Plan Mode
            // (monthPlanId != null)
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
                            monthPlan.getId());

                    String errorMessage = String.format("Item's time slot (%s) does not match month plan (%s)",
                            timeSlotMonthStr, monthPlanMonthStr);

                    log.warn("Month mismatch: timeSlot={}, monthPlan={}",
                            timeSlotMonthStr, monthPlanMonthStr);
                    return new BaseResponse<>(0, errorMessage, errorDetails);
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
                                monthPlan.getId());

                        MonthPlanErrorResponse errorResponse = new MonthPlanErrorResponse(
                                false,
                                "INVALID_MONTH_PLAN_REFERENCE",
                                String.format("Routine '%s' is not approved in the %s month plan",
                                        request.getName(), requestedMonth),
                                errorDetails);

                        log.warn("Routine not approved in month plan: name={}, monthPlanId={}",
                                request.getName(), monthPlan.getId());
                        return new BaseResponse<>(0, errorResponse.getMessage(), errorResponse);
                    }

                    // ===== FALLBACK: Check for existing unscheduled routine =====
                    // If a routine with the same name and monthPlanId already exists but is
                    // unscheduled,
                    // directly update it instead of creating a duplicate
                    Optional<CalendarItem> existingUnscheduledRoutine = calendarItemRepository.findAllByUserId(userId)
                            .stream()
                            .filter(item -> item instanceof Routine)
                            .filter(item -> item.getMonthPlanId() != null
                                    && item.getMonthPlanId().equals(request.getMonthPlanId()))
                            .filter(item -> item.getName() != null && item.getName().equals(request.getName()))
                            .filter(item -> item.getTimeSlot() == null || item.getTimeSlot().getStartTime() == null)
                            .findFirst();

                    if (existingUnscheduledRoutine.isPresent()) {
                        Routine existingRoutine = (Routine) existingUnscheduledRoutine.get();
                        log.info("Found existing unscheduled routine, updating directly: routineId={}, name={}",
                                existingRoutine.getId(), request.getName());

                        // Update timeSlot
                        if (request.getTimeSlot() != null) {
                            TimeSlot newTimeSlot = new TimeSlot(
                                    request.getTimeSlot().getStartTime(),
                                    request.getTimeSlot().getEndTime());
                            existingRoutine.setTimeSlot(newTimeSlot);
                        }

                        // Update pattern - derive from timeSlot if not provided
                        RecurringPattern pattern = existingRoutine.getPattern();
                        if (pattern == null) {
                            pattern = new RecurringPattern();
                        }

                        boolean hasPatternFromRequest = request.getRoutineDetails() != null
                                && request.getRoutineDetails().getPattern() != null
                                && request.getRoutineDetails().getPattern().getDaysOfWeek() != null
                                && !request.getRoutineDetails().getPattern().getDaysOfWeek().isEmpty();

                        if (hasPatternFromRequest) {
                            // Use pattern from request
                            List<DayOfWeek> daysOfWeek = request.getRoutineDetails().getPattern().getDaysOfWeek()
                                    .stream()
                                    .map(day -> {
                                        try {
                                            return DayOfWeek.valueOf(day.toUpperCase());
                                        } catch (IllegalArgumentException e) {
                                            return null;
                                        }
                                    })
                                    .filter(day -> day != null)
                                    .collect(Collectors.toList());
                            if (!daysOfWeek.isEmpty()) {
                                pattern.setDaysOfWeek(daysOfWeek);
                            }
                        } else if (request.getTimeSlot() != null && request.getTimeSlot().getStartTime() != null) {
                            // Derive day of week from the drop date
                            DayOfWeek dropDay = request.getTimeSlot().getStartTime().getDayOfWeek();
                            pattern.setDaysOfWeek(List.of(dropDay));
                            log.info("Pattern not provided, derived from drop date: {}", dropDay);
                        }

                        existingRoutine.setPattern(pattern);

                        // Update other fields if provided
                        if (request.getNote() != null) {
                            existingRoutine.setNote(request.getNote());
                        }
                        if (request.getColor() != null) {
                            existingRoutine.setColor(request.getColor());
                        }

                        // Set weekPlanId for the item
                        existingRoutine.setWeekPlanId(weekPlanId);

                        // Save and return success
                        CalendarItem savedItem = calendarItemRepository.save(existingRoutine);
                        log.info("Existing unscheduled routine updated successfully: routineId={}", savedItem.getId());

                        return new BaseResponse<>(1, "Routine scheduled successfully", savedItem.getId());
                    }
                    // ===== END FALLBACK =====

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
                                monthPlan.getId());

                        MonthPlanErrorResponse errorResponse = new MonthPlanErrorResponse(
                                false,
                                "INVALID_MONTH_PLAN_REFERENCE",
                                String.format("Parent big task ID %d not found in %s month plan",
                                        parentBigTaskId, requestedMonth),
                                errorDetails);

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

                // Validate scheduled date is within project task date range
                PM_TasKDTO pmTask = res.get();
                if (request.getTimeSlot() != null && request.getTimeSlot().getStartTime() != null) {
                    LocalDate scheduledDate = request.getTimeSlot().getStartTime().toLocalDate();
                    LocalDate taskStartDate = pmTask.getStartDate();
                    LocalDate taskEndDate = pmTask.getEndDate();

                    // Check if scheduled date is outside the valid range
                    boolean isBeforeStart = taskStartDate != null && scheduledDate.isBefore(taskStartDate);
                    boolean isAfterEnd = taskEndDate != null && scheduledDate.isAfter(taskEndDate);

                    if (isBeforeStart || isAfterEnd) {
                        String startDateStr = taskStartDate != null ? taskStartDate.toString() : "N/A";
                        String endDateStr = taskEndDate != null ? taskEndDate.toString() : "N/A";

                        String warningMessage = String.format(
                                "This task must be scheduled between %s and %s. The selected date (%s) is outside the valid range.",
                                startDateStr, endDateStr, scheduledDate.toString());

                        log.warn(
                                "Project task date range violation: pmTaskId={}, scheduledDate={}, validRange={} to {}",
                                request.getPmTaskId(), scheduledDate, startDateStr, endDateStr);

                        return new BaseResponse<>(0, warningMessage, null);
                    }
                }
            }

            // 5. Validate constraints (overlapping, sleep hours, daily limits)
            if (request.getTimeSlot() != null) {
                List<String> violations = constraintValidationService.validateConstraints(
                        userId,
                        request.getTimeSlot().getStartTime(),
                        request.getTimeSlot().getEndTime(),
                        itemType);

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
                    Constant.MSG_ITEM_CREATED_SUCCESS);

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
     * Checks if a new item request overlaps with any existing scheduled routines
     * for CreateCalendarItemRequest.
     *
     * @param request           The new item being created.
     * @param scheduledRoutines A list of existing, scheduled routines for the user.
     * @return An Optional containing an error message if an overlap is found.
     */
    private Optional<String> createRequestFindRoutineOverlap(CreateCalendarItemRequest request,
            List<Routine> scheduledRoutines) {
        return findRoutineOverlap(
                ItemType.valueOf(request.getType().toUpperCase()),
                request.getTimeSlot(),
                request.getRoutineDetails(), // Can be null if not a routine or no details
                null, // No exclude ID for creation
                scheduledRoutines);
    }

    /**
     * Checks if a new item request overlaps with any existing scheduled routines
     * for UpdateCalendarItemRequest.
     *
     * @param request           The item being updated.
     * @param scheduledRoutines A list of existing, scheduled routines for the user.
     * @return An Optional containing an error message if an overlap is found.
     */
    private Optional<String> updateRequestFindRoutineOverlap(UpdateCalendarItemRequest request, Long updateItemId,
            List<Routine> scheduledRoutines) {
        return findRoutineOverlap(
                getUpdateRequestItemType(request),
                request.getTimeSlot(),
                request.getRoutineDetails(),
                updateItemId,
                scheduledRoutines);
    }

    /**
     * Unified helper method to check for overlaps with existing scheduled routines.
     * Handles both single-occurrence items (Task/Event) and recurring items
     * (Routine).
     */
    private Optional<String> findRoutineOverlap(
            ItemType newItemType,
            TimeSlotDTO newItemTimeSlot,
            RoutineDetailsDTO newRoutineDetails,
            Long excludeItemId,
            List<Routine> scheduledRoutines) {
        return findRoutineOverlap(newItemType, newItemTimeSlot, newRoutineDetails,
                excludeItemId, scheduledRoutines, null);
    }

    /**
     * Unified helper method to check for overlaps with existing scheduled routines.
     * Handles both single-occurrence items (Task/Event) and recurring items
     * (Routine).
     *
     * @param newItemType       The type of the new item.
     * @param newItemTimeSlot   The time slot of the new item.
     * @param newRoutineDetails The routine details (pattern) if the new item is a
     *                          routine.
     * @param excludeItemId     The ID of the item to exclude from overlap checks
     *                          (self).
     * @param scheduledRoutines The list of existing scheduled routines.
     * @param exceptionDate     When detaching a routine occurrence, this date
     *                          should be
     *                          excluded from the parent routine's overlap check.
     * @return An Optional containing an error message if an overlap is found.
     */
    private Optional<String> findRoutineOverlap(
            ItemType newItemType,
            TimeSlotDTO newItemTimeSlot,
            RoutineDetailsDTO newRoutineDetails,
            Long excludeItemId,
            List<Routine> scheduledRoutines,
            LocalDate exceptionDate) {

        // If the new item isn't scheduled, it can't overlap.
        if (newItemTimeSlot == null || newItemTimeSlot.getStartTime() == null) {
            return Optional.empty();
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        // Prepare list of routines to compare
        List<Routine> routinesToCompare = scheduledRoutines;
        if (excludeItemId != null) {
            routinesToCompare = scheduledRoutines.stream()
                    .filter(routine -> !routine.getId().equals(excludeItemId))
                    .collect(Collectors.toList());
        }

        if (routinesToCompare.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime newItemStartTime = newItemTimeSlot.getStartTime();
        LocalDateTime newItemEndTime = newItemTimeSlot.getEndTime();
        LocalDate newItemDate = newItemStartTime.toLocalDate();

        // Check Case 2: New item is a Recurring Routine (Pattern vs Pattern)
        // We only check Pattern vs Pattern if it IS a Routine AND has a valid pattern
        // in details
        boolean isRecurringRoutine = newItemType == ItemType.ROUTINE &&
                newRoutineDetails != null &&
                newRoutineDetails.getPattern() != null &&
                newRoutineDetails.getPattern().getDaysOfWeek() != null &&
                !newRoutineDetails.getPattern().getDaysOfWeek().isEmpty();

        if (isRecurringRoutine) {
            // --- Case 2: New item is a RECURRING ROUTINE ---
            LocalTime newRoutineTimeStart = newItemStartTime.toLocalTime();
            LocalTime newRoutineTimeEnd = newItemEndTime.toLocalTime();
            List<DayOfWeek> newDays = newRoutineDetails.getPattern().getDaysOfWeek().stream()
                    .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                    .collect(Collectors.toList());

            for (Routine existingRoutine : routinesToCompare) {
                // 0. Skip future routines (haven't started when new routine starts)
                if (existingRoutine.getEndDate() != null &&
                        !existingRoutine.getEndDate().toLocalDate().isAfter(newItemDate)) {
                    continue;
                }
                // Skip routines that start AFTER the new routine (Assuming new routine starts
                // 'now')
                LocalDate routineStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                if (newItemDate.isBefore(routineStartDate)) {
                    continue;
                }

                // Check Pattern Overlap
                if (existingRoutine.getPattern() == null || existingRoutine.getPattern().getDaysOfWeek() == null
                        || existingRoutine.getPattern().getDaysOfWeek().isEmpty()) {
                    continue; // Standalone routine
                }

                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                boolean dayOfWeekOverlaps = newDays.stream().anyMatch(existingDays::contains);

                if (dayOfWeekOverlaps) {
                    LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                    LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                    if (timesOverlap(newRoutineTimeStart, newRoutineTimeEnd, existingStartTime, existingEndTime)) {
                        String overlappingDay = newDays.stream()
                                .filter(existingDays::contains)
                                .findFirst()
                                .map(day -> day.toString().charAt(0) + day.toString().substring(1).toLowerCase())
                                .orElse("a recurring day");

                        return Optional.of(String.format(
                                "New routine pattern overlaps with existing routine '%s' on %s (%s - %s)",
                                existingRoutine.getName(),
                                overlappingDay,
                                existingStartTime.format(timeFormatter),
                                existingEndTime.format(timeFormatter)));
                    }
                }
            }

        } else {
            // --- Case 1: New item is TASK / EVENT / STANDALONE ROUTINE / ROUTINE w/o DET
            // ---
            // Check specific date vs Routine Pattern

            DayOfWeek newDayOfWeek = newItemDate.getDayOfWeek();
            LocalTime newItemTimeStart = newItemStartTime.toLocalTime();
            LocalTime newItemTimeEnd = newItemEndTime.toLocalTime();

            for (Routine existingRoutine : routinesToCompare) {
                // 0. Skip ended routines
                if (existingRoutine.getEndDate() != null &&
                        !existingRoutine.getEndDate().toLocalDate().isAfter(newItemDate)) {
                    continue;
                }

                // 1. Skip future routines
                LocalDate routineStartDate = existingRoutine.getTimeSlot().getStartTime().toLocalDate();
                if (newItemDate.isBefore(routineStartDate)) {
                    continue;
                }

                // 3. Check specific day overlap
                if (existingRoutine.getPattern() == null || existingRoutine.getPattern().getDaysOfWeek() == null
                        || existingRoutine.getPattern().getDaysOfWeek().isEmpty()) {
                    continue;
                }

                // 4. SKIP if this is a detach operation and the new item's date matches
                // the exception date. This prevents false positives when detaching an
                // occurrence from its parent routine.
                if (exceptionDate != null && newItemDate.equals(exceptionDate)) {
                    // The parent routine will have an exception added for this date,
                    // so there's no real overlap.
                    continue;
                }

                List<DayOfWeek> existingDays = existingRoutine.getPattern().getDaysOfWeek();
                if (existingDays.contains(newDayOfWeek)) {
                    // 5. SKIP if the parent routine has an exception for this specific date
                    // This handles the case where a standalone routine was created by detaching
                    // an occurrence from this parent routine.
                    if (existingRoutine.getExceptions() != null && !existingRoutine.getExceptions().isEmpty()) {
                        boolean hasExceptionForThisDate = existingRoutine.getExceptions().stream()
                                .anyMatch(exDate -> exDate.toLocalDate().equals(newItemDate));
                        if (hasExceptionForThisDate) {
                            // The parent routine has an exception for this date,
                            // so there's no real overlap on this date.
                            continue;
                        }
                    }

                    LocalTime existingStartTime = existingRoutine.getTimeSlot().getStartTime().toLocalTime();
                    LocalTime existingEndTime = existingRoutine.getTimeSlot().getEndTime().toLocalTime();

                    if (timesOverlap(newItemTimeStart, newItemTimeEnd, existingStartTime, existingEndTime)) {
                        String friendlyDayOfWeek = newDayOfWeek.toString().charAt(0)
                                + newDayOfWeek.toString().substring(1).toLowerCase();

                        return Optional.of(String.format(
                                "New %s overlaps with existing routine '%s' on %s (%s - %s)",
                                newItemType.name().toLowerCase(),
                                existingRoutine.getName(),
                                friendlyDayOfWeek,
                                existingStartTime.format(timeFormatter),
                                existingEndTime.format(timeFormatter)));
                    }
                }
            }
        }
        return Optional.empty();
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
                    dto.getEndTime());
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
                        Optional<String> routineOverlapError = updateRequestFindRoutineOverlap(request, itemId,
                                scheduledRoutines);
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
                        item.getType());

                if (!violations.isEmpty()) {
                    log.warn(Constant.LOG_CONSTRAINT_VIOLATIONS, userId, violations);
                    return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, violations);
                }

                // ===== PM TASK DATE RANGE VALIDATION =====
                // Validate that the new scheduled date is within the PM task's date range
                if (item instanceof com.graduation.schedulingservice.model.ProjectTask) {
                    com.graduation.schedulingservice.model.ProjectTask projectTask = (com.graduation.schedulingservice.model.ProjectTask) item;
                    Long pmTaskId = projectTask.getPmTaskId();

                    if (pmTaskId != null) {
                        Optional<PM_TasKDTO> pmTaskOpt = projectServiceClient.getProjectTaskById(pmTaskId);
                        if (pmTaskOpt.isPresent()) {
                            PM_TasKDTO pmTask = pmTaskOpt.get();
                            LocalDate scheduledDate = request.getTimeSlot().getStartTime().toLocalDate();
                            LocalDate taskStartDate = pmTask.getStartDate();
                            LocalDate taskEndDate = pmTask.getEndDate();

                            // Check if scheduled date is outside the valid range
                            boolean isBeforeStart = taskStartDate != null && scheduledDate.isBefore(taskStartDate);
                            boolean isAfterEnd = taskEndDate != null && scheduledDate.isAfter(taskEndDate);

                            if (isBeforeStart || isAfterEnd) {
                                String startDateStr = taskStartDate != null ? taskStartDate.toString() : "N/A";
                                String endDateStr = taskEndDate != null ? taskEndDate.toString() : "N/A";

                                String warningMessage = String.format(
                                        "This task must be scheduled between %s and %s. The selected date (%s) is outside the valid range.",
                                        startDateStr, endDateStr, scheduledDate.toString());

                                log.warn(
                                        "Project task date range violation on update: pmTaskId={}, scheduledDate={}, validRange={} to {}",
                                        pmTaskId, scheduledDate, startDateStr, endDateStr);

                                return new BaseResponse<>(0, warningMessage, null);
                            }
                        }
                    }
                }
                // ===== END PM TASK DATE RANGE VALIDATION =====
            }

            // 5. Update common fields
            // For Routines, we do NOT update in-place because we handle "Split Series"
            // later.
            // We want the Old Routine to keep its original values (History).
            if (!(item instanceof Routine)) {
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
            } else if (item instanceof Routine) {
                Routine oldRoutine = (Routine) item;
                RecurringPattern oldPattern = oldRoutine.getPattern();

                // Check if this is a STANDALONE routine (no recurring pattern)
                // Standalone routines should be updated in-place, not split
                boolean isStandalone = oldPattern == null
                        || oldPattern.getDaysOfWeek() == null
                        || oldPattern.getDaysOfWeek().isEmpty();

                if (isStandalone) {
                    // UPDATE IN-PLACE for standalone routines (like Tasks/Events)
                    log.info("Updating standalone routine ID={} in-place", oldRoutine.getId());

                    if (request.getName() != null) {
                        oldRoutine.setName(request.getName());
                    }
                    if (request.getNote() != null) {
                        oldRoutine.setNote(request.getNote());
                    }
                    if (request.getColor() != null) {
                        oldRoutine.setColor(request.getColor());
                    }
                    if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                        try {
                            oldRoutine.setStatus(ItemStatus.valueOf(request.getStatus().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            log.warn(Constant.LOG_INVALID_STATUS_UPDATE, request.getStatus());
                            return new BaseResponse<>(0, Constant.MSG_INVALID_STATUS_VALUE, null);
                        }
                    }
                    if (request.getTimeSlot() != null) {
                        TimeSlotDTO dto = request.getTimeSlot();
                        TimeSlot timeSlot = new TimeSlot(dto.getStartTime(), dto.getEndTime());
                        oldRoutine.setTimeSlot(timeSlot);
                    }

                    // Update pattern - this is needed when scheduling an unscheduled routine
                    if (request.getRoutineDetails() != null && request.getRoutineDetails().getPattern() != null) {
                        var patternDTO = request.getRoutineDetails().getPattern();
                        if (patternDTO.getDaysOfWeek() != null && !patternDTO.getDaysOfWeek().isEmpty()) {
                            RecurringPattern pattern = oldRoutine.getPattern();
                            if (pattern == null) {
                                pattern = new RecurringPattern();
                            }
                            var daysOfWeek = patternDTO.getDaysOfWeek().stream()
                                    .map(day -> {
                                        try {
                                            return DayOfWeek.valueOf(day.toUpperCase());
                                        } catch (IllegalArgumentException e) {
                                            return null;
                                        }
                                    })
                                    .filter(day -> day != null)
                                    .collect(Collectors.toList());
                            if (!daysOfWeek.isEmpty()) {
                                pattern.setDaysOfWeek(daysOfWeek);
                                oldRoutine.setPattern(pattern);
                                log.info("Pattern set for routine ID={}: daysOfWeek={}",
                                        oldRoutine.getId(), daysOfWeek);
                            }
                        }
                    } else if (oldRoutine.getPattern() == null && request.getTimeSlot() != null) {
                        // Fallback: derive day from timeSlot if no pattern provided
                        DayOfWeek dropDay = request.getTimeSlot().getStartTime().getDayOfWeek();
                        RecurringPattern pattern = new RecurringPattern();
                        pattern.setDaysOfWeek(List.of(dropDay));
                        oldRoutine.setPattern(pattern);
                        log.info("Pattern derived from drop date for routine ID={}: dayOfWeek={}",
                                oldRoutine.getId(), dropDay);
                    }

                    // Save and return - standalone routine updated in place
                    CalendarItem updatedRoutine = calendarItemRepository.save(oldRoutine);
                    log.info("Standalone routine ID={} updated successfully", updatedRoutine.getId());
                    return new BaseResponse<>(1, Constant.MSG_ITEM_UPDATE_SUCCESS, updatedRoutine.getId());
                }

                // SPLIT SERIES LOGIC for RECURRING routines:
                // Instead of updating in-place, we terminate the old routine and create a new
                // one.

                // 1. Determine Split Time
                LocalDateTime splitTime;
                if (request.getTimeSlot() != null) {
                    // Set end date to START of the split day to ensure the old routine
                    // does not generate an instance on this day.
                    splitTime = request.getTimeSlot().getStartTime().toLocalDate().atStartOfDay();
                } else {
                    // Fallback using server time
                    splitTime = LocalDateTime.now().toLocalDate().atStartOfDay();
                }

                // 2. Terminate Old Routine
                // End date is set to the split time.
                oldRoutine.setEndDate(splitTime);
                calendarItemRepository.save(oldRoutine);

                // 3. Create New Routine
                Routine newRoutine = new Routine();
                newRoutine.setUserId(userId);
                newRoutine.setCalendarId(oldRoutine.getCalendarId());
                newRoutine.setMonthPlanId(oldRoutine.getMonthPlanId());
                newRoutine.setWeekPlanId(oldRoutine.getWeekPlanId());
                newRoutine.setMemorableEventId(oldRoutine.getMemorableEventId());

                newRoutine.setName(request.getName() != null ? request.getName() : oldRoutine.getName());
                newRoutine.setNote(request.getNote() != null ? request.getNote() : oldRoutine.getNote());
                newRoutine.setColor(request.getColor() != null ? request.getColor() : oldRoutine.getColor());
                newRoutine.setStatus(
                        (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                                ? ItemStatus.valueOf(request.getStatus().toUpperCase())
                                : oldRoutine.getStatus());

                // TimeSlot
                if (request.getTimeSlot() != null) {
                    newRoutine.setTimeSlot(
                            new TimeSlot(request.getTimeSlot().getStartTime(), request.getTimeSlot().getEndTime()));
                } else {
                    newRoutine.setTimeSlot(oldRoutine.getTimeSlot());
                }

                // Pattern
                RecurringPattern newPattern = new RecurringPattern();

                if (request.getRoutineDetails() != null && request.getRoutineDetails().getPattern() != null) {
                    var patternDTO = request.getRoutineDetails().getPattern();
                    if (patternDTO.getDaysOfWeek() != null && !patternDTO.getDaysOfWeek().isEmpty()) {
                        var daysOfWeek = patternDTO.getDaysOfWeek().stream()
                                .map(day -> {
                                    try {
                                        return DayOfWeek.valueOf(day.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        return null;
                                    }
                                })
                                .filter(day -> day != null)
                                .collect(Collectors.toList());
                        newPattern.setDaysOfWeek(daysOfWeek);
                    }
                } else if (oldPattern != null) {
                    // Copy old pattern if not provided in request
                    newPattern.setDaysOfWeek(new ArrayList<>(oldPattern.getDaysOfWeek()));
                }
                newRoutine.setPattern(newPattern);

                // 3.5. Transfer valid exceptions from old routine to new routine
                // Exceptions that fall on or after the split time should be transferred
                // to the new routine (e.g., detached occurrences that were created before
                // this update)
                if (oldRoutine.getExceptions() != null && !oldRoutine.getExceptions().isEmpty()) {
                    LocalDate splitDate = splitTime.toLocalDate();
                    for (LocalDateTime exceptionDate : oldRoutine.getExceptions()) {
                        // Only transfer exceptions that are on or after the split date
                        if (!exceptionDate.toLocalDate().isBefore(splitDate)) {
                            newRoutine.addException(exceptionDate);
                            log.info("Transferred exception {} from old routine {} to new routine",
                                    exceptionDate, oldRoutine.getId());
                        }
                    }
                }

                // 4. Save New Routine
                CalendarItem savedNewRoutine = calendarItemRepository.save(newRoutine);

                log.info("Routine split successfully. Old ID: {}, New ID: {}", oldRoutine.getId(),
                        savedNewRoutine.getId());
                return new BaseResponse<>(1, Constant.MSG_ITEM_UPDATE_SUCCESS, savedNewRoutine.getId());
            }

            // 7. Save updated item
            CalendarItem updatedItem = calendarItemRepository.save(item);

            log.info(Constant.LOG_ITEM_UPDATED_SUCCESS, itemId);
            return new BaseResponse<>(1, Constant.MSG_ITEM_UPDATE_SUCCESS, updatedItem.getId());

        } catch (

        Exception e) {
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
        return validateConstraintsForUpdate(userId, itemId, startTime, endTime, itemType, null);
    }

    /**
     * Validate constraints for update operation with exception date support.
     * When detaching a routine occurrence, we need to exclude the specific date
     * from the parent routine's overlap check.
     *
     * @param userId        The user's ID.
     * @param itemId        The ID of the item being updated/detached from.
     * @param startTime     The new start time.
     * @param endTime       The new end time.
     * @param itemType      The item's type.
     * @param exceptionDate The date being detached (to skip overlap check for this
     *                      date on the parent routine). Can be null for normal
     *                      updates.
     * @return A list of violation messages.
     */
    private List<String> validateConstraintsForUpdate(Long userId, Long itemId,
            LocalDateTime startTime, LocalDateTime endTime,
            ItemType itemType, LocalDate exceptionDate) {

        // --- FIX: ---
        // 1. Call a method that validates *only* non-overlap constraints (like sleep,
        // daily limits).
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
                                    itemEnd.format(timeFormatter));
                        } else {
                            timeString = String.format("%s - %s",
                                    itemStart.format(fullFormatter),
                                    itemEnd.format(fullFormatter));
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

        // This check applies to all item types (TASK, EVENT, ROUTINE)
        // When a ROUTINE is being created/detached, we need to check against other
        // routines too

        // Get all existing recurring routines (with pattern)
        List<Routine> scheduledRoutines = calendarItemRepository.findAllByUserId(userId).stream()
                .filter(calItem -> calItem instanceof Routine)
                .map(calItem -> (Routine) calItem)
                .filter(Routine::isScheduled) // Has a pattern and timeslot
                .collect(Collectors.toList());

        // Create a temporary TimeSlotDTO for the check
        TimeSlotDTO checkTimeSlot = new TimeSlotDTO(startTime, endTime);

        // REPLACED BLOCK: Use the unified helper with exception date support
        Optional<String> routineOverlapMsg = findRoutineOverlap(
                itemType,
                checkTimeSlot,
                null, // treat as single occurrence check even if routine
                itemId, // exclude self
                scheduledRoutines,
                exceptionDate); // exclude this date when checking parent routine

        if (routineOverlapMsg.isPresent()) {
            violations.add(routineOverlapMsg.get());
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
                    item.getTimeSlot().getEndTime()));
        }

        // Type-specific fields
        if (item instanceof Task) {
            Task task = (Task) item;
            dto.setParentBigTaskId(task.getParentBigTaskId());

            // Look up the parent big task name if parentBigTaskId exists
            if (task.getParentBigTaskId() != null) {
                bigTaskRepository.findById(task.getParentBigTaskId())
                        .ifPresent(bigTask -> dto.setParentBigTaskName(bigTask.getName()));
            }

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
                            subtask.getCompletedAt()))
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
            if (referenceDate.isAfter(LocalDate.now().plusYears(5))
                    || referenceDate.isBefore(LocalDate.now().minusYears(5))) {
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
            List<CalendarItem> nonRecurringItems = calendarItemRepository
                    .findScheduledItemsExcludingRoutinesByDateRange(
                            userId, calendarIds, dateRange.getStart(), dateRange.getEnd());

            // 6. Add Task, Event items to the list
            nonRecurringItems.stream()
                    .map(this::mapToScheduledItemDTO)
                    .forEach(finalItemList::add);

            // 7. Fetch data from repository (Recurring Routines)
            // This gets all potentially active recurring routines
            List<Routine> recurringRoutines = calendarItemRepository.findActiveRecurringRoutines(
                    userId, calendarIds, dateRange.getStart(), dateRange.getEnd());

            // 8. Filter routines whose active month overlaps the date range
            LocalDate rangeStartDate = dateRange.getStart().toLocalDate();
            LocalDate rangeEndDate = dateRange.getEnd().toLocalDate();

            List<Routine> activeRoutines = recurringRoutines.stream()
                    .filter(routine -> {
                        LocalDate routineActiveStartDate = routine.getTimeSlot().getStartTime().toLocalDate();

                        // Treat routine as infinite (no end date check)
                        // It is active if it started on or before the range end date.
                        boolean overlaps = (routineActiveStartDate.isBefore(rangeEndDate)
                                || routineActiveStartDate.isEqual(rangeEndDate));

                        return overlaps;
                    })
                    .collect(Collectors.toList());

            // 9. Add the *original* Routine objects (not expanded)
            activeRoutines.stream()
                    .map(this::mapToScheduledItemDTO)
                    .forEach(finalItemList::add);

            // 9.5. Fetch and add Standalone Routines (detached single occurrences)
            List<Routine> standaloneRoutines = calendarItemRepository.findStandaloneRoutines(
                    userId, calendarIds, dateRange.getStart(), dateRange.getEnd());
            standaloneRoutines.stream()
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
                    item.getTimeSlot().getEndTime()));
        }
        if (item instanceof Routine) {
            Routine routine = (Routine) item;
            dto.setPattern(routine.getPattern());
            dto.setExceptions(routine.getExceptions());
            dto.setEndDate(routine.getEndDate());
        }
        // Populate parentBigTaskId and parentBigTaskName for Task items
        if (item instanceof Task) {
            Task task = (Task) item;
            dto.setParentBigTaskId(task.getParentBigTaskId());

            // Look up the parent big task name
            if (task.getParentBigTaskId() != null) {
                bigTaskRepository.findById(task.getParentBigTaskId())
                        .ifPresent(bigTask -> dto.setParentBigTaskName(bigTask.getName()));
            }
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
                    item.getType());

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

        return new BaseResponse<>(1, "Batch schedule operation completed.",
                new BatchScheduleResponse(true, scheduledCount, constraintViolations));
    }

    @Override
    @Transactional
    public BaseResponse<?> detachRoutineInstance(Long userId, Long routineId,
            com.graduation.schedulingservice.payload.request.DetachRoutineRequest request) {
        try {
            log.info("Detaching routine instance: userId={}, routineId={}, exceptionDate={}",
                    userId, routineId, request.getExceptionDate());

            // 1. Find the original routine
            java.util.Optional<CalendarItem> itemOpt = calendarItemRepository.findById(routineId);
            if (itemOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_ITEM_NOT_FOUND, null);
            }

            CalendarItem item = itemOpt.get();
            if (!(item instanceof Routine)) {
                return new BaseResponse<>(0, "Item is not a routine", null);
            }
            Routine routine = (Routine) item;

            // 2. Verify ownership
            if (!routine.getUserId().equals(userId)) {
                return new BaseResponse<>(0, Constant.MSG_UNAUTHORIZED_ACCESS, null);
            }

            // 3. Prepare the new standalone item details
            CreateCalendarItemRequest newDetails = request.getNewDetails();
            if (newDetails != null && "ROUTINE".equalsIgnoreCase(newDetails.getType())
                    && newDetails.getRoutineDetails() != null) {
                newDetails.getRoutineDetails().setPattern(null);
            }

            // 4. VALIDATE CONSTRAINTS FIRST before modifying anything
            // This prevents the routine from disappearing if the new item would cause
            // overlap
            if (newDetails != null && newDetails.getTimeSlot() != null) {
                TimeSlotDTO timeSlotDTO = newDetails.getTimeSlot();

                // Validate start < end
                if (timeSlotDTO.getEndTime().isBefore(timeSlotDTO.getStartTime()) ||
                        timeSlotDTO.getEndTime().isEqual(timeSlotDTO.getStartTime())) {
                    log.warn("Invalid time slot for detach: {} to {}",
                            timeSlotDTO.getStartTime(), timeSlotDTO.getEndTime());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_SLOT, null);
                }

                // Check for overlaps with existing items (excluding the routine being detached
                // from AND the specific date being detached)
                LocalDate exceptionDateForValidation = request.getExceptionDate() != null
                        ? request.getExceptionDate().toLocalDate()
                        : null;
                List<String> violations = validateConstraintsForUpdate(
                        userId,
                        routineId, // Exclude this routine from overlap check
                        timeSlotDTO.getStartTime(),
                        timeSlotDTO.getEndTime(),
                        ItemType.ROUTINE,
                        exceptionDateForValidation); // Skip overlap check for this specific date

                if (!violations.isEmpty()) {
                    log.warn("Constraint violations for detach operation: {}", violations);
                    return new BaseResponse<>(0, Constant.MSG_CONSTRAINT_VIOLATIONS, violations);
                }
            }

            // 5. Check if there's already an existing standalone routine for this
            // occurrence date
            // If so, delete it to prevent duplicates (user is re-detaching/editing the same
            // occurrence)
            LocalDateTime exceptionDate = request.getExceptionDate();
            if (exceptionDate != null && request.getNewDetails() != null
                    && "ROUTINE".equalsIgnoreCase(request.getNewDetails().getType())) {

                // Find existing standalone routines (no pattern) for the same user, at the same
                // time
                List<CalendarItem> existingStandaloneRoutines = calendarItemRepository.findAllByUserId(userId).stream()
                        .filter(calItem -> calItem instanceof Routine)
                        .map(calItem -> (Routine) calItem)
                        .filter(r -> {
                            // Must be a standalone routine (no pattern or empty pattern)
                            boolean isStandalone = r.getPattern() == null
                                    || r.getPattern().getDaysOfWeek() == null
                                    || r.getPattern().getDaysOfWeek().isEmpty();
                            if (!isStandalone)
                                return false;

                            // Must have the same start time as the exception date
                            if (r.getTimeSlot() == null || r.getTimeSlot().getStartTime() == null)
                                return false;

                            // Compare start time (date and hour/minute)
                            LocalDateTime routineStart = r.getTimeSlot().getStartTime();
                            boolean sameDateTime = routineStart.toLocalDate().equals(exceptionDate.toLocalDate())
                                    && routineStart.toLocalTime().getHour() == exceptionDate.toLocalTime().getHour()
                                    && routineStart.toLocalTime().getMinute() == exceptionDate.toLocalTime()
                                            .getMinute();

                            return sameDateTime;
                        })
                        .collect(java.util.stream.Collectors.toList());

                // Delete any existing standalone routines found for this occurrence
                if (!existingStandaloneRoutines.isEmpty()) {
                    log.info(
                            "Found {} existing standalone routine(s) for occurrence at {}. Deleting before creating replacement.",
                            existingStandaloneRoutines.size(), exceptionDate);
                    for (CalendarItem existing : existingStandaloneRoutines) {
                        calendarItemRepository.delete(existing);
                        log.info("Deleted existing standalone routine ID={}", existing.getId());
                    }
                }
            }

            // 6. NOW add exception to the original routine (after validation passed)
            routine.addException(request.getExceptionDate());
            calendarItemRepository.save(routine);

            // 7. Create the new standalone item DIRECTLY (skip createItem's overlap check
            // since we already validated with the exception date context)
            log.info("Creating standalone routine after successful validation: {}", newDetails.toString());

            // Create standalone routine directly
            Routine standaloneRoutine = new Routine();
            standaloneRoutine.setUserId(userId);
            standaloneRoutine.setCalendarId(newDetails.getCalendarId());
            standaloneRoutine.setName(newDetails.getName());
            standaloneRoutine.setNote(newDetails.getNote());
            standaloneRoutine.setColor(newDetails.getColor());
            standaloneRoutine.setStatus(ItemStatus.INCOMPLETE);

            if (newDetails.getTimeSlot() != null) {
                TimeSlotDTO dto = newDetails.getTimeSlot();
                TimeSlot timeSlot = new TimeSlot(dto.getStartTime(), dto.getEndTime());
                standaloneRoutine.setTimeSlot(timeSlot);
            }

            // Standalone routines have NO pattern (that's what makes them standalone)
            standaloneRoutine.setPattern(null);

            // Save the standalone routine
            CalendarItem savedItem = calendarItemRepository.save(standaloneRoutine);

            log.info("Standalone routine created successfully: id={}", savedItem.getId());

            CreateItemResponse response = new CreateItemResponse(
                    true,
                    savedItem.getId(),
                    "Routine occurrence detached successfully");

            return new BaseResponse<>(1, "Routine occurrence detached successfully", response);

        } catch (Exception e) {
            log.error("Failed to detach routine instance", e);
            return new BaseResponse<>(0, "Failed to detach routine instance", null);
        }
    }

}