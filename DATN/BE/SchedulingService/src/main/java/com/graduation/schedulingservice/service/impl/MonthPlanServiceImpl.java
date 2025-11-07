package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.model.enums.PlanStatus;
import com.graduation.schedulingservice.model.enums.TaskStatus;
import com.graduation.schedulingservice.payload.request.*;
import com.graduation.schedulingservice.payload.request.UnscheduledTaskDTO;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.*;
import com.graduation.schedulingservice.service.ConstraintValidationService;
import com.graduation.schedulingservice.service.MonthPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthPlanServiceImpl implements MonthPlanService {

    private final ConstraintValidationService constraintValidationService;
    private final MonthPlanRepository monthPlanRepository;
    private final WeekPlanRepository weekPlanRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final BigTaskRepository bigTaskRepository;
    private final CalendarRepository calendarRepository;

    @Override
    @Transactional
    public BaseResponse<?> createMonthPlan(Long userId, CreateMonthPlanRequest request) {
        try {
            log.info("Creating month plan: userId={}, year={}, month={}",
                    userId, request.getYear(), request.getMonth());

            // 1. Check if month plan already exists
            if (monthPlanRepository.existsByUserIdAndYearAndMonth(userId, request.getYear(), request.getMonth())) {
                log.warn("Month plan already exists: userId={}, year={}, month={}",
                        userId, request.getYear(), request.getMonth());
                return new BaseResponse<>(0, "Month plan already exists for this period", null);
            }

            // 2. Create new month plan
            MonthPlan monthPlan = new MonthPlan();
            monthPlan.setUserId(userId);
            monthPlan.setYear(request.getYear());
            monthPlan.setMonth(request.getMonth());
            monthPlan.setStatus(PlanStatus.DRAFT);

            // 3. Auto-copy routines from previous month
            List<String> approvedRoutines = copyRoutinesFromPreviousMonth(userId, request.getYear(), request.getMonth());
            monthPlan.setApprovedRoutineNames(approvedRoutines);

            // 4. Save month plan
            MonthPlan savedMonthPlan = monthPlanRepository.save(monthPlan);

            // 5. Create week plans (4-5 weeks per month)
            List<Long> weekPlanIds = createWeekPlans(savedMonthPlan.getId(), request.getYear(), request.getMonth());

            // 6. Build response
            CreateMonthPlanResponse response = new CreateMonthPlanResponse(
                    savedMonthPlan.getId(),
                    approvedRoutines,
                    weekPlanIds,
                    "Month plan created"
            );

            log.info("Month plan created successfully: monthPlanId={}", savedMonthPlan.getId());
            return new BaseResponse<>(1, "Month plan created successfully", response);

        } catch (Exception e) {
            log.error("Failed to create month plan: userId={}, year={}, month={}",
                    userId, request.getYear(), request.getMonth(), e);
            return new BaseResponse<>(0, "Failed to create month plan", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getMonthPlan(Long userId, Long monthPlanId) {
        try {
            log.info("Getting month plan: userId={}, monthPlanId={}", userId, monthPlanId);

            // 1. Find month plan
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(monthPlanId, userId);

            if (monthPlanOpt.isEmpty()) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found", null);
            }

            MonthPlan monthPlan = monthPlanOpt.get();

            // --- OPTIMIZATION ---
            // 1.5. Get all calendar items for this user ONCE to avoid multiple DB calls
            List<CalendarItem> allUserItems = calendarItemRepository.findAllByUserId(userId);

            // 2. Build response
            MonthPlanResponse response = new MonthPlanResponse();
            response.setId(monthPlan.getId());
            response.setYear(monthPlan.getYear());
            response.setMonth(monthPlan.getMonth());
            response.setStatus(monthPlan.getStatus().name());

            // --- MODIFIED LOGIC ---
            // 2.5. Get unscheduled routines for this month
            // This now filters all user items for Routines that match this monthPlanId and are unscheduled (no time slot)
            List<String> unscheduledRoutineNames = allUserItems.stream()
                    .filter(item -> item instanceof Routine) // Filter for Routine type
                    .filter(item -> item.getMonthPlanId() != null && item.getMonthPlanId().equals(monthPlanId)) // Filter for this month plan
                    .map(item -> (Routine) item) // Cast to Routine
                    .filter(routine -> routine.getTimeSlot() == null || routine.getTimeSlot().getStartTime() == null) // Filter for unscheduled
                    .map(Routine::getName) // Get the name
                    .collect(Collectors.toList());

            response.setApprovedRoutineNames(unscheduledRoutineNames);

            // 3. Get big tasks with derived tasks count and completion percentage
            List<BigTaskDTO> bigTaskDTOs = monthPlan.getBigTasks().stream()
                    .map(bigTask -> {
                        BigTaskDTO dto = new BigTaskDTO();
                        dto.setId(bigTask.getId());
                        dto.setName(bigTask.getName());
                        dto.setEstimatedStartDate(bigTask.getEstimatedStartDate());
                        dto.setEstimatedEndDate(bigTask.getEstimatedEndDate());

                        // Count derived tasks (Tasks with parentBigTaskId = bigTask.id)
                        // Use the prefetched allUserItems list
                        List<CalendarItem> derivedTasks = allUserItems.stream()
                                .filter(item -> item instanceof Task)
                                .filter(item -> ((Task) item).getParentBigTaskId() != null)
                                .filter(item -> ((Task) item).getParentBigTaskId().equals(bigTask.getId()))
                                .collect(Collectors.toList());

                        dto.setDerivedTasksCount(derivedTasks.size());

                        // Calculate completion percentage
                        if (derivedTasks.isEmpty()) {
                            dto.setCompletionPercentage(0);
                        } else {
                            long completedCount = derivedTasks.stream()
                                    .map(item -> (Task) item)
                                    .filter(task -> task.getStatus().name().equals("COMPLETE"))
                                    .count();
                            dto.setCompletionPercentage((int) ((completedCount * 100) / derivedTasks.size()));
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            response.setBigTasks(bigTaskDTOs);

            // 4. Get events for this month (Events with monthPlanId = this monthPlan.id)
            // Use the prefetched allUserItems list
            List<EventDTO> eventDTOs = allUserItems.stream()
                    .filter(item -> item instanceof Event)
                    .filter(item -> item.getMonthPlanId() != null)
                    .filter(item -> item.getMonthPlanId().equals(monthPlanId))
                    .map(item -> {
                        Event event = (Event) item;
                        EventDTO dto = new EventDTO();
                        dto.setId(event.getId());
                        dto.setName(event.getName());

                        // Get specific date from timeSlot
                        if (event.getTimeSlot() != null && event.getTimeSlot().getStartTime() != null) {
                            dto.setSpecificDate(event.getTimeSlot().getStartTime().toLocalDate());
                            dto.setStartTime(event.getTimeSlot().getStartTime().toLocalTime());
                            dto.setEndTime(event.getTimeSlot().getEndTime().toLocalTime());
                            dto.setIsScheduled(true);
                        } else {
                            dto.setIsScheduled(false);
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            response.setEvents(eventDTOs);

            // 5. Get week plans
            List<WeekPlanDTO> weekPlanDTOs = weekPlanRepository.findByMonthPlanIdOrderByWeekNumberAsc(monthPlanId).stream()
                    .map(weekPlan -> {
                        WeekPlanDTO dto = new WeekPlanDTO();
                        dto.setWeekNumber(weekPlan.getWeekNumber());
                        dto.setWeekPlanId(weekPlan.getId());
                        dto.setStatus(weekPlan.getStatus().name());
                        return dto;
                    })
                    .collect(Collectors.toList());

            response.setWeekPlans(weekPlanDTOs);

            log.info("Month plan retrieved successfully: monthPlanId={}", monthPlanId);
            return new BaseResponse<>(1, "Month plan retrieved successfully", response);

        } catch (Exception e) {
            log.error("Failed to get month plan: monthPlanId={}, userId={}", monthPlanId, userId, e);
            return new BaseResponse<>(0, "Failed to retrieve month plan", null);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getMonthPlanIdByDate(Long userId, int year, int month) {
        try {
            log.info("Finding month plan ID by date: userId={}, year={}, month={}",
                    userId, year, month);

            // 1. Find month plan by user, year, and month
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByUserIdAndYearAndMonth(
                    userId, year, month);

            if (monthPlanOpt.isPresent()) {
                // 2. Found it, return the ID
                Long monthPlanId = monthPlanOpt.get().getId();
                log.info("Found month plan ID: {}", monthPlanId);
                return new BaseResponse<>(1, "Month plan ID retrieved successfully", monthPlanId);
            } else {
                // 3. Not found, return a specific message
                log.warn("No month plan found for userId={}, year={}, month={}",
                        userId, year, month);
                return new BaseResponse<>(0, "Month plan not found for the specified date", null);
            }

        } catch (Exception e) {
            log.error("Failed to find month plan ID by date: userId={}, year={}, month={}",
                    userId, year, month, e);
            return new BaseResponse<>(0, "Failed to retrieve month plan ID", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> addBigTask(Long userId, Long monthPlanId, AddBigTaskRequest request) {
        try {
            log.info("Adding big task to month plan: userId={}, monthPlanId={}, taskName={}",
                    userId, monthPlanId, request.getName());

            // 1. Validate month plan exists and belongs to user
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(monthPlanId, userId);
            if (monthPlanOpt.isEmpty()) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            MonthPlan monthPlan = monthPlanOpt.get();

            // 1.5. Find a default calendar for the user
            // Unscheduled tasks must be linked to a calendar
            List<Calendar> userCalendars = calendarRepository.findByUserId(userId);
            if (userCalendars.isEmpty()) {
                log.warn("No calendars found for user: userId={}", userId);
                return new BaseResponse<>(0, "No calendar found for this user. Cannot create tasks.", null);
            }
            Long defaultCalendarId = userCalendars.get(0).getId(); // Use the first calendar found

            // 2. Calculate month date range
            LocalDate[] monthRange = getMonthDateRange(monthPlan.getYear(), monthPlan.getMonth());
            LocalDate firstDayOfMonth = monthRange[0];
            LocalDate lastDayOfMonth = monthRange[1];

            // 3. Validate date range
            if (request.getEstimatedStartDate().isAfter(request.getEstimatedEndDate())) {
                log.warn("Invalid date range: start date is after end date");
                return new BaseResponse<>(0, "Start date must be before or equal to end date", null);
            }

            // 4. Validate dates are within month range
            if (request.getEstimatedStartDate().isBefore(firstDayOfMonth) ||
                    request.getEstimatedEndDate().isAfter(lastDayOfMonth)) {
                log.warn("Big task dates outside month range: taskDates=[{}, {}], monthRange=[{}, {}]",
                        request.getEstimatedStartDate(), request.getEstimatedEndDate(),
                        firstDayOfMonth, lastDayOfMonth);
                return new BaseResponse<>(0,
                        String.format("Big task dates must be within month range: %s to %s",
                                firstDayOfMonth, lastDayOfMonth),
                        null);
            }

            // 5. Create big task
            BigTask bigTask = new BigTask();
            bigTask.setName(request.getName());
            bigTask.setDescription(request.getDescription());
            bigTask.setEstimatedStartDate(request.getEstimatedStartDate());
            bigTask.setEstimatedEndDate(request.getEstimatedEndDate());
            bigTask.setStatus(TaskStatus.NOT_STARTED);
            bigTask.setMonthPlan(monthPlan);

            // 6. Save big task
            BigTask savedBigTask = bigTaskRepository.save(bigTask);

            // 6.5. Create and save unscheduled tasks (if any)
            List<Task> savedUnscheduledTasks = new ArrayList<>(); // Store the saved tasks
            if (request.getUnscheduledTasks() != null && !request.getUnscheduledTasks().isEmpty()) {
                List<Task> unscheduledTasks = new ArrayList<>();
                for (UnscheduledTaskDTO dto : request.getUnscheduledTasks()) {
                    Task task = new Task();
                    task.setName(dto.getName());
                    task.setNote(dto.getNote());
                    task.setUserId(userId);
                    task.setCalendarId(defaultCalendarId); // Set the default calendar ID
                    task.setMonthPlanId(monthPlanId); // Link to this month plan
                    task.setParentBigTaskId(savedBigTask.getId()); // Link to the new big task
                    task.setStatus(ItemStatus.INCOMPLETE);
                    // timeSlot remains null, making it unscheduled
                    unscheduledTasks.add(task);
                }
                savedUnscheduledTasks = calendarItemRepository.saveAll(unscheduledTasks);
                log.info("Created {} unscheduled tasks for bigTaskId={}", unscheduledTasks.size(), savedBigTask.getId());
            }

            // 7. Find affected week plans (any overlap with date range)
            List<WeekPlan> affectedWeekPlans = weekPlanRepository.findByMonthPlanIdAndDateRangeOverlap(
                    monthPlanId,
                    request.getEstimatedStartDate(),
                    request.getEstimatedEndDate()
            );

            List<Long> affectedWeekPlanIds = affectedWeekPlans.stream()
                    .map(WeekPlan::getId)
                    .collect(Collectors.toList());

            // 8. Build response
            List<UnscheduledTaskResponseDTO> unscheduledTaskDTOs = savedUnscheduledTasks.stream()
                    .map(task -> new UnscheduledTaskResponseDTO(task.getId(), task.getName(), task.getNote()))
                    .collect(Collectors.toList());

            // 9. Build response
            AddBigTaskResponse response = new AddBigTaskResponse(
                    savedBigTask.getId(),
                    "Big task added successfully",
                    affectedWeekPlanIds,
                    unscheduledTaskDTOs
            );

            log.info("Big task added successfully: bigTaskId={}, affectedWeeks={}",
                    savedBigTask.getId(), affectedWeekPlanIds.size());

            return new BaseResponse<>(1, "Big task added successfully", response);

        } catch (Exception e) {
            log.error("Failed to add big task: monthPlanId={}, userId={}", monthPlanId, userId, e);
            return new BaseResponse<>(0, "Failed to add big task", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getBigTaskById(Long userId, Long monthPlanId, Long bigTaskId) {
        try {
            log.info("Getting big task by ID: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // 1. Validate month plan exists and belongs to user
            if (!monthPlanRepository.existsByIdAndUserId(monthPlanId, userId)) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            // 2. Find big task by ID
            Optional<BigTask> bigTaskOpt = bigTaskRepository.findById(bigTaskId);
            if (bigTaskOpt.isEmpty()) {
                log.warn("Big task not found: bigTaskId={}", bigTaskId);
                return new BaseResponse<>(0, "Big task not found", null);
            }

            BigTask bigTask = bigTaskOpt.get();

            // 3. Validate big task belongs to the specified month plan
            if (bigTask.getMonthPlan() == null || !bigTask.getMonthPlan().getId().equals(monthPlanId)) {
                log.warn("Big task {} does not belong to month plan {}", bigTaskId, monthPlanId);
                return new BaseResponse<>(0, "Big task not found in this month plan", null);
            }

            // 4. Fetch derived tasks (both scheduled and unscheduled)
            List<Task> derivedTasks = calendarItemRepository.findAllByUserId(userId).stream()
                    .filter(item -> item instanceof Task)
                    .map(item -> (Task) item) // Cast to Task
                    .filter(task -> task.getParentBigTaskId() != null && task.getParentBigTaskId().equals(bigTask.getId()))
                    .collect(Collectors.toList());

            // 5. Build BigTaskDTO
            BigTaskDTO bigTaskDTO = mapToBigTaskDTO(bigTask, derivedTasks);

            // 6. Build Unscheduled Tasks List
            List<UnscheduledTaskResponseDTO> unscheduledTasks = derivedTasks.stream()
                    .filter(task -> !task.isScheduled()) // Filter for unscheduled
                    .map(task -> new UnscheduledTaskResponseDTO(task.getId(), task.getName(), task.getNote()))
                    .collect(Collectors.toList());

            // 7. Build final response DTO
            GetBigTaskResponseDTO responseDTO = new GetBigTaskResponseDTO(bigTaskDTO, unscheduledTasks);

            log.info("Big task retrieved successfully: bigTaskId={}", bigTaskId);
            return new BaseResponse<>(1, "Big task retrieved successfully", responseDTO);

        } catch (Exception e) {
            log.error("Failed to get big task: bigTaskId={}, userId={}", bigTaskId, userId, e);
            return new BaseResponse<>(0, "Failed to retrieve big task", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateBigTask(Long userId, Long monthPlanId, Long bigTaskId, UpdateBigTaskRequest request) {
        try {
            log.info("Updating big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // 1. Validate month plan exists and belongs to user
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(monthPlanId, userId);
            if (monthPlanOpt.isEmpty()) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            MonthPlan monthPlan = monthPlanOpt.get();

            // 2. Find big task
            Optional<BigTask> bigTaskOpt = bigTaskRepository.findById(bigTaskId);
            if (bigTaskOpt.isEmpty()) {
                log.warn("Big task not found: bigTaskId={}", bigTaskId);
                return new BaseResponse<>(0, "Big task not found", null);
            }

            BigTask bigTask = bigTaskOpt.get();

            // 3. Validate big task belongs to the month plan
            if (bigTask.getMonthPlan() == null || !bigTask.getMonthPlan().getId().equals(monthPlanId)) {
                log.warn("Big task {} does not belong to month plan {}", bigTaskId, monthPlanId);
                return new BaseResponse<>(0, "Big task not found in this month plan", null);
            }

            // 4. Calculate month date range
            LocalDate[] monthRange = getMonthDateRange(monthPlan.getYear(), monthPlan.getMonth());
            LocalDate firstDayOfMonth = monthRange[0];
            LocalDate lastDayOfMonth = monthRange[1];

            // 5. Validate new date range
            if (request.getEstimatedStartDate().isAfter(request.getEstimatedEndDate())) {
                log.warn("Invalid date range: start date is after end date");
                return new BaseResponse<>(0, "Start date must be before or equal to end date", null);
            }

            // 6. Validate new dates are within month range
            if (request.getEstimatedStartDate().isBefore(firstDayOfMonth) ||
                    request.getEstimatedEndDate().isAfter(lastDayOfMonth)) {
                log.warn("Big task dates outside month range: taskDates=[{}, {}], monthRange=[{}, {}]",
                        request.getEstimatedStartDate(), request.getEstimatedEndDate(),
                        firstDayOfMonth, lastDayOfMonth);
                return new BaseResponse<>(0,
                        String.format("Big task dates must be within month range: %s to %s",
                                firstDayOfMonth, lastDayOfMonth),
                        null);
            }

            // 7. Update big task fields
            bigTask.setName(request.getName());
            bigTask.setDescription(request.getDescription());
            bigTask.setEstimatedStartDate(request.getEstimatedStartDate());
            bigTask.setEstimatedEndDate(request.getEstimatedEndDate());

            // 8. Save updated big task
            BigTask savedBigTask = bigTaskRepository.save(bigTask);

            // 9. Build response DTO
            // We need to fetch the derived tasks to build the DTO correctly
            List<Task> derivedTasks = calendarItemRepository.findAllByUserId(userId).stream()
                    .filter(item -> item instanceof Task)
                    .map(item -> (Task) item)
                    .filter(task -> task.getParentBigTaskId() != null && task.getParentBigTaskId().equals(savedBigTask.getId()))
                    .collect(Collectors.toList());

            // Validate new date range against *scheduled* derived tasks
            List<Task> scheduledDerivedTasks = derivedTasks.stream()
                    .filter(Task::isScheduled)
                    .collect(Collectors.toList());

            if (!scheduledDerivedTasks.isEmpty()) {
                LocalDate newBigTaskStart = request.getEstimatedStartDate();
                LocalDate newBigTaskEnd = request.getEstimatedEndDate();

                for (Task scheduledTask : scheduledDerivedTasks) {
                    LocalDate taskDate = scheduledTask.getTimeSlot().getStartTime().toLocalDate();

                    // Check if the scheduled task's date is outside the new proposed range
                    if (taskDate.isBefore(newBigTaskStart) || taskDate.isAfter(newBigTaskEnd)) {
                        log.warn("Big task update conflicts with scheduled sub-task: bigTaskId={}, taskId={}, taskDate={}, newRange=[{}, {}]",
                                bigTaskId, scheduledTask.getId(), taskDate, newBigTaskStart, newBigTaskEnd);

                        String errorMessage = String.format(
                                "Update failed: Scheduled sub-task '%s' (on %s) would be outside the new date range of %s to %s.",
                                scheduledTask.getName(), taskDate, newBigTaskStart, newBigTaskEnd
                        );
                        return new BaseResponse<>(0, errorMessage, null);
                    }
                }
            }

            BigTaskDTO responseDTO = mapToBigTaskDTO(savedBigTask, derivedTasks);

            log.info("Big task updated successfully: bigTaskId={}", savedBigTask.getId());
            return new BaseResponse<>(1, "Big task updated successfully", responseDTO);

        } catch (Exception e) {
            log.error("Failed to update big task: bigTaskId={}", bigTaskId, e);
            return new BaseResponse<>(0, "Failed to update big task", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> addUnscheduledTask(Long userId, Long monthPlanId, Long bigTaskId, AddUnscheduledTaskRequest request) {
        try {
            log.info("Adding unscheduled task to big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // 1. Validate Month Plan and user ownership
            if (!monthPlanRepository.existsByIdAndUserId(monthPlanId, userId)) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            // 2. Validate Big Task and its link to the Month Plan
            Optional<BigTask> bigTaskOpt = bigTaskRepository.findById(bigTaskId);
            if (bigTaskOpt.isEmpty() || !bigTaskOpt.get().getMonthPlan().getId().equals(monthPlanId)) {
                log.warn("Big task not found or not in specified month plan: bigTaskId={}, monthPlanId={}",
                        bigTaskId, monthPlanId);
                return new BaseResponse<>(0, "Big task not found in this month plan", null);
            }

            // 3. Get user's default calendar
            List<Calendar> userCalendars = calendarRepository.findByUserId(userId);
            if (userCalendars.isEmpty()) {
                log.warn("No calendars found for user: userId={}", userId);
                return new BaseResponse<>(0, "No calendar found for this user. Cannot create tasks.", null);
            }
            Calendar defaultCalendar = userCalendars.get(0);

            // 4. Create new unscheduled task
            Task unscheduledTask = new Task();
            unscheduledTask.setUserId(userId);
            unscheduledTask.setCalendarId(defaultCalendar.getId());
            unscheduledTask.setMonthPlanId(monthPlanId);
            unscheduledTask.setName(request.getName());
            unscheduledTask.setNote(request.getNote());
            unscheduledTask.setStatus(ItemStatus.INCOMPLETE);
            unscheduledTask.setParentBigTaskId(bigTaskId); // Link to parent
            // TimeSlot remains null

            // 5. Save task
            Task savedTask = calendarItemRepository.save(unscheduledTask);

            // 6. Build response
            UnscheduledTaskResponseDTO responseDTO = new UnscheduledTaskResponseDTO(
                    savedTask.getId(),
                    savedTask.getName(),
                    savedTask.getNote()
            );

            log.info("Unscheduled task added successfully: unscheduledTaskId={}", savedTask.getId());
            return new BaseResponse<>(1, "Unscheduled task added successfully", responseDTO);

        } catch (Exception e) {
            log.error("Failed to add unscheduled task: bigTaskId={}", bigTaskId, e);
            return new BaseResponse<>(0, "Failed to add unscheduled task", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateUnscheduledTask(Long userId, Long monthPlanId, Long bigTaskId, Long unscheduledTaskId, UpdateUnscheduledTaskRequest request) {
        try {
            log.info("Updating unscheduled task: userId={}, monthPlanId={}, bigTaskId={}, unscheduledTaskId={}",
                    userId, monthPlanId, bigTaskId, unscheduledTaskId);

            // 1. Validate Month Plan and user ownership
            if (!monthPlanRepository.existsByIdAndUserId(monthPlanId, userId)) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            // 2. Validate Big Task and its link to the Month Plan
            Optional<BigTask> bigTaskOpt = bigTaskRepository.findById(bigTaskId);
            if (bigTaskOpt.isEmpty() || !bigTaskOpt.get().getMonthPlan().getId().equals(monthPlanId)) {
                log.warn("Big task not found or not in specified month plan: bigTaskId={}, monthPlanId={}",
                        bigTaskId, monthPlanId);
                return new BaseResponse<>(0, "Big task not found in this month plan", null);
            }

            // 3. Find the unscheduled task (as a CalendarItem)
            Optional<CalendarItem> itemOpt = calendarItemRepository.findById(unscheduledTaskId);
            if (itemOpt.isEmpty()) {
                log.warn("Unscheduled task (CalendarItem) not found: unscheduledTaskId={}", unscheduledTaskId);
                return new BaseResponse<>(0, "Unscheduled task not found", null);
            }

            CalendarItem item = itemOpt.get();

            // 4. Validate user ownership of the task
            if (!item.getUserId().equals(userId)) {
                log.warn("Unauthorized access to task: unscheduledTaskId={}, userId={}", unscheduledTaskId, userId);
                return new BaseResponse<>(0, "Unauthorized access to task", null);
            }

            // 5. Validate it's an unscheduled Task
            if (!(item instanceof Task)) {
                log.warn("Item is not a Task: unscheduledTaskId={}", unscheduledTaskId);
                return new BaseResponse<>(0, "Item is not a task", null);
            }

            Task task = (Task) item;
            if (task.isScheduled()) {
                log.warn("Task is already scheduled, cannot update as 'unscheduled': unscheduledTaskId={}", unscheduledTaskId);
                return new BaseResponse<>(0, "Task is already scheduled", null);
            }

            // 6. Validate parentBigTaskId matches
            if (task.getParentBigTaskId() == null || !task.getParentBigTaskId().equals(bigTaskId)) {
                log.warn("Task parentBigTaskId {} does not match path bigTaskId {}",
                        task.getParentBigTaskId(), bigTaskId);
                return new BaseResponse<>(0, "Task is not a child of the specified big task", null);
            }

            // 7. All validations passed. Update the task.
            task.setName(request.getName());
            task.setNote(request.getNote());

            Task updatedTask = calendarItemRepository.save(task);

            // 8. Build response
            UnscheduledTaskResponseDTO responseDTO = new UnscheduledTaskResponseDTO(
                    updatedTask.getId(),
                    updatedTask.getName(),
                    updatedTask.getNote()
            );

            log.info("Unscheduled task updated successfully: unscheduledTaskId={}", updatedTask.getId());
            return new BaseResponse<>(1, "Unscheduled task updated successfully", responseDTO);

        } catch (Exception e) {
            log.error("Failed to update unscheduled task: unscheduledTaskId={}", unscheduledTaskId, e);
            return new BaseResponse<>(0, "Failed to update unscheduled task", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteBigTask(Long userId, Long monthPlanId, Long bigTaskId) {
        try {
            log.info("Deleting big task: userId={}, monthPlanId={}, bigTaskId={}",
                    userId, monthPlanId, bigTaskId);

            // 1. Validate month plan exists and belongs to user
            if (!monthPlanRepository.existsByIdAndUserId(monthPlanId, userId)) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            // 2. Find big task by ID
            Optional<BigTask> bigTaskOpt = bigTaskRepository.findById(bigTaskId);
            if (bigTaskOpt.isEmpty()) {
                log.warn("Big task not found: bigTaskId={}", bigTaskId);
                return new BaseResponse<>(0, "Big task not found", null);
            }

            BigTask bigTask = bigTaskOpt.get();

            // 3. Validate big task belongs to the specified month plan and user
            if (bigTask.getMonthPlan() == null || !bigTask.getMonthPlan().getId().equals(monthPlanId) || !bigTask.getMonthPlan().getUserId().equals(userId)) {
                log.warn("Big task {} does not belong to month plan {} or user {}", bigTaskId, monthPlanId, userId);
                return new BaseResponse<>(0, "Big task not found in this month plan", null);
            }

            // 4. Find all derived tasks (scheduled and unscheduled)
            List<Task> derivedTasks = calendarItemRepository.findAllByUserId(userId).stream()
                    .filter(item -> item instanceof Task)
                    .map(item -> (Task) item)
                    .filter(task -> task.getParentBigTaskId() != null && task.getParentBigTaskId().equals(bigTask.getId()))
                    .collect(Collectors.toList());

            // 5. Delete all derived tasks
            if (!derivedTasks.isEmpty()) {
                calendarItemRepository.deleteAll(derivedTasks);
                log.info("Deleted {} derived tasks for bigTaskId={}", derivedTasks.size(), bigTaskId);
            }

            // 6. Delete the big task itself
            bigTaskRepository.delete(bigTask);

            log.info("Big task deleted successfully: bigTaskId={}", bigTaskId);
            return new BaseResponse<>(1, "Big task and all its derived tasks deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete big task: bigTaskId={}", bigTaskId, e);
            return new BaseResponse<>(0, "Failed to delete big task", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> addEvent(Long userId, Long monthPlanId, AddEventRequest request) {
        try {
            log.info("Adding event to month plan: userId={}, monthPlanId={}, eventName={}",
                    userId, monthPlanId, request.getName());

            // 1. Validate month plan exists and belongs to user
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(monthPlanId, userId);
            if (monthPlanOpt.isEmpty()) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            MonthPlan monthPlan = monthPlanOpt.get();

            // 2. Calculate month date range
            LocalDate[] monthRange = getMonthDateRange(monthPlan.getYear(), monthPlan.getMonth());
            LocalDate firstDayOfMonth = monthRange[0];
            LocalDate lastDayOfMonth = monthRange[1];

            // 3. Validate time range
            if (request.getStartTime().isAfter(request.getEndTime()) ||
                    request.getStartTime().equals(request.getEndTime())) {
                log.warn("Invalid time range: start time must be before end time");
                return new BaseResponse<>(0, "Start time must be before end time", null);
            }

            // 4. Validate event date is within month range
            if (request.getSpecificDate().isBefore(firstDayOfMonth) ||
                    request.getSpecificDate().isAfter(lastDayOfMonth)) {
                log.warn("Event date outside month range: eventDate={}, monthRange=[{}, {}]",
                        request.getSpecificDate(), firstDayOfMonth, lastDayOfMonth);
                return new BaseResponse<>(0,
                        String.format("Event date must be within month range: %s to %s",
                                firstDayOfMonth, lastDayOfMonth),
                        null);
            }

            // 5. Validate calendar exists and belongs to user
            Optional<Calendar> calendarOpt = calendarRepository.findById(request.getCalendarId());
            if (calendarOpt.isEmpty()) {
                log.warn("Calendar not found: calendarId={}", request.getCalendarId());
                return new BaseResponse<>(0, "Calendar not found", null);
            }

            Calendar calendar = calendarOpt.get();
            if (!calendar.getUserId().equals(userId)) {
                log.warn("Calendar does not belong to user: calendarId={}, userId={}", request.getCalendarId(), userId);
                return new BaseResponse<>(0, "Unauthorized access to calendar", null);
            }

//            // 6. Check for duplicate event on the same date in this month plan
//            boolean duplicateExists = calendarItemRepository.findAllByUserId(userId).stream()
//                    .filter(item -> item instanceof Event)
//                    .filter(item -> item.getMonthPlanId() != null)
//                    .filter(item -> item.getMonthPlanId().equals(monthPlanId))
//                    .filter(item -> item.getTimeSlot() != null && item.getTimeSlot().getStartTime() != null)
//                    .anyMatch(item -> item.getTimeSlot().getStartTime().toLocalDate().equals(request.getSpecificDate()));
//
//            if (duplicateExists) {
//                log.warn("Event already exists on this date in month plan: date={}, monthPlanId={}",
//                        request.getSpecificDate(), monthPlanId);
//                return new BaseResponse<>(0,
//                        "An event already exists on " + request.getSpecificDate() + " in this month plan",
//                        null);
//            }

            // 7. Find the week plan that contains this date
            Optional<WeekPlan> weekPlanOpt = weekPlanRepository.findByMonthPlanIdAndDateWithin(
                    monthPlanId,
                    request.getSpecificDate()
            );

            if (weekPlanOpt.isEmpty()) {
                log.warn("No week plan found for date: date={}, monthPlanId={}",
                        request.getSpecificDate(), monthPlanId);
                return new BaseResponse<>(0, "No week plan found for the specified date", null);
            }

            WeekPlan weekPlan = weekPlanOpt.get();

            // 8. Create Event entity
            Event event = new Event();
            event.setUserId(userId);
            event.setCalendarId(request.getCalendarId());
            event.setMonthPlanId(monthPlanId);
            event.setWeekPlanId(weekPlan.getId());
            event.setName(request.getName());
            event.setNote(request.getNote());
            event.setStatus(ItemStatus.INCOMPLETE);

            // 9. Create TimeSlot (auto-schedule)
            LocalDateTime startDateTime = LocalDateTime.of(request.getSpecificDate(), request.getStartTime());
            LocalDateTime endDateTime = LocalDateTime.of(request.getSpecificDate(), request.getEndTime());

            // Validate constraints
            List<String> violations = constraintValidationService.validateConstraints(
                    userId,
                    startDateTime,
                    endDateTime,
                    ItemType.EVENT
            );

            if (!violations.isEmpty()) {
                return new BaseResponse<>(0, "Constraint violations", violations);
            }

            TimeSlot timeSlot = new TimeSlot(startDateTime, endDateTime);
            event.setTimeSlot(timeSlot);

            // 10. Save event
            Event savedEvent = calendarItemRepository.save(event);

            // 11. Build response
            AddEventResponse response = new AddEventResponse(
                    savedEvent.getId(),
                    savedEvent.getId(), // calendarItemId is same as eventId
                    "Event added and scheduled successfully",
                    weekPlan.getId()
            );

            log.info("Event added successfully: eventId={}, weekPlanId={}", savedEvent.getId(), weekPlan.getId());

            return new BaseResponse<>(1, "Event added and scheduled successfully", response);

        } catch (Exception e) {
            log.error("Failed to add event: monthPlanId={}, userId={}", monthPlanId, userId, e);
            return new BaseResponse<>(0, "Failed to add event", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateRoutineList(Long userId, Long monthPlanId, UpdateRoutineListRequest request) {
        try {
            log.info("Updating routine list for month plan: userId={}, monthPlanId={}", userId, monthPlanId);

            // 1. Validate month plan exists and belongs to user
            Optional<MonthPlan> monthPlanOpt = monthPlanRepository.findByIdAndUserId(monthPlanId, userId);
            if (monthPlanOpt.isEmpty()) {
                log.warn("Month plan not found or unauthorized: monthPlanId={}, userId={}", monthPlanId, userId);
                return new BaseResponse<>(0, "Month plan not found or unauthorized", null);
            }

            MonthPlan monthPlan = monthPlanOpt.get();

            // 2. Get current routine names
            Set<String> oldRoutines = new HashSet<>(monthPlan.getApprovedRoutineNames());
            Set<String> newRoutines = new HashSet<>(request.getApprovedRoutineNames());

            // 3. Calculate differences
            Set<String> removedRoutines = new HashSet<>(oldRoutines);
            removedRoutines.removeAll(newRoutines);

            Set<String> addedRoutines = new HashSet<>(newRoutines);
            addedRoutines.removeAll(oldRoutines);

            // 4. Update the routine list
            monthPlan.setApprovedRoutineNames(new ArrayList<>(request.getApprovedRoutineNames()));

            // 4a. Handle removed routines: Delete them
            if (!removedRoutines.isEmpty()) {
                // Find all routines for this user and month plan that match the removed names
                List<Routine> routinesToDelete = calendarItemRepository.findAllByUserId(userId).stream()
                        .filter(item -> item instanceof Routine) // Is a Routine
                        .map(item -> (Routine) item)
                        .filter(item -> monthPlanId.equals(item.getMonthPlanId())) // Belongs to this month
                        .filter(item -> removedRoutines.contains(item.getName())) // Name is in the removed list
                        .collect(Collectors.toList());

                if (!routinesToDelete.isEmpty()) {
                    calendarItemRepository.deleteAll(routinesToDelete);
                    log.info("Deleted {} removed routines from calendar for monthPlanId={}", routinesToDelete.size(), monthPlanId);
                }
            }

            // 4b. Handle added routines: Create them (with default calendar and status)
            if (!addedRoutines.isEmpty()) {
                // Find a default calendar for the user
                List<Calendar> userCalendars = calendarRepository.findByUserId(userId);
                if (userCalendars.isEmpty()) {
                    log.warn("No calendars found for user: userId={}. Cannot create new routines.", userId);
                    return new BaseResponse<>(0, "No calendar found for this user. Cannot create routines.", null);
                }
                Long defaultCalendarId = userCalendars.get(0).getId(); // Use the first calendar found
                // --- END NEW LOGIC ---

                List<Routine> routinesToCreate = new ArrayList<>();
                for (String routineName : addedRoutines) {
                    Routine newRoutine = new Routine();
                    newRoutine.setUserId(userId);
                    newRoutine.setMonthPlanId(monthPlanId);
                    newRoutine.setName(routineName);
                    newRoutine.setCalendarId(defaultCalendarId); // Set default calendar
                    newRoutine.setStatus(ItemStatus.INCOMPLETE); // Set default status
                    newRoutine.setWeekPlanId(null); // Explicitly set to null as it's unscheduled
                    // TimeSlot remains null, making it unscheduled
                    routinesToCreate.add(newRoutine);
                }
                calendarItemRepository.saveAll(routinesToCreate);
                log.info("Created {} new routines in calendar for monthPlanId={}", routinesToCreate.size(), monthPlanId);
            }

            // 5. Save changes
            monthPlanRepository.save(monthPlan);

            // 6. Build response
            UpdateRoutineListResponse response = new UpdateRoutineListResponse(
                    "Routine list updated successfully",
                    new ArrayList<>(removedRoutines),
                    new ArrayList<>(addedRoutines)
            );

            log.info("Routine list updated: monthPlanId={}, added={}, removed={}",
                    monthPlanId, addedRoutines.size(), removedRoutines.size());

            return new BaseResponse<>(1, "Routine list updated successfully", response);

        } catch (Exception e) {
            log.error("Failed to update routine list: monthPlanId={}, userId={}", monthPlanId, userId, e);
            return new BaseResponse<>(0, "Failed to update routine list", null);
        }
    }

    /**
     * Copy approved routine names from the previous month
     */
    private List<String> copyRoutinesFromPreviousMonth(Long userId, Integer year, Integer month) {
        try {
            // Calculate previous month
            int prevMonth = month - 1;
            int prevYear = year;
            if (prevMonth == 0) {
                prevMonth = 12;
                prevYear = year - 1;
            }

            // Find previous month plan
            Optional<MonthPlan> prevMonthPlanOpt = monthPlanRepository.findByUserIdAndYearAndMonth(
                    userId, prevYear, prevMonth);

            if (prevMonthPlanOpt.isPresent()) {
                List<String> routines = prevMonthPlanOpt.get().getApprovedRoutineNames();
                log.info("Copied {} routines from previous month: year={}, month={}",
                        routines.size(), prevYear, prevMonth);
                return new ArrayList<>(routines);
            } else {
                log.info("No previous month plan found, starting with empty routine list");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("Failed to copy routines from previous month, starting with empty list", e);
            return new ArrayList<>();
        }
    }

    /**
     * Creates full Monday-to-Sunday week plans for a given month's planning period.
     * The first week starts on the Monday containing the 1st of the month.
     * The final week created is the one containing the last day of the month.
     * This ensures all days of the month are included in a weekly plan.
     *
     * @param monthPlanId The ID of the parent month plan.
     * @param year        The target year.
     * @param month       The target month.
     * @return A list of IDs for the newly created WeekPlan entities.
     */
    private List<Long> createWeekPlans(Long monthPlanId, Integer year, Integer month) {
        List<Long> weekPlanIds = new ArrayList<>();
        int weekNumber = 1;

        // 1. Define the calendar boundaries of the month.
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 2. Determine the start of the very first week.
        // This is the Monday of the week containing the first day of the month.
        LocalDate currentWeekStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 3. Loop as long as the start of the week is not after the month's last day.
        // This ensures we create the week that contains the last day.
        while (!currentWeekStart.isAfter(lastDayOfMonth)) {
            // The end of the current week is always 6 days after the start (Sunday).
            LocalDate currentWeekEnd = currentWeekStart.plusDays(6);

            // 4. Create and save the WeekPlan entity.
            WeekPlan weekPlan = new WeekPlan();
            weekPlan.setMonthPlanId(monthPlanId);
            weekPlan.setWeekNumber(weekNumber);
            weekPlan.setStartDate(currentWeekStart);
            weekPlan.setEndDate(currentWeekEnd);
            weekPlan.setStatus(PlanStatus.DRAFT);

            WeekPlan saved = weekPlanRepository.save(weekPlan);
            weekPlanIds.add(saved.getId());

            log.info("Created week plan: weekNumber={}, startDate={}, endDate={}",
                    weekNumber, currentWeekStart, currentWeekEnd);

            // 5. Move to the start of the next week and increment the counter.
            currentWeekStart = currentWeekStart.plusWeeks(1);
            weekNumber++;
        }

        return weekPlanIds;
    }

    /**
     * Private helper to build a BigTaskDTO from a BigTask entity.
     * This includes calculating derived task counts and completion percentage.
     */
    private BigTaskDTO mapToBigTaskDTO(BigTask bigTask, List<Task> derivedTasks) {
        BigTaskDTO dto = new BigTaskDTO();
        dto.setId(bigTask.getId());
        dto.setName(bigTask.getName());
        dto.setEstimatedStartDate(bigTask.getEstimatedStartDate());
        dto.setEstimatedEndDate(bigTask.getEstimatedEndDate());

        // Use the provided derivedTasks list
        dto.setDerivedTasksCount(derivedTasks.size());

        // Calculate completion percentage based on *scheduled* derived tasks
        List<Task> scheduledDerivedTasks = derivedTasks.stream()
                .filter(Task::isScheduled) // Use the helper method
                .collect(Collectors.toList());

        if (scheduledDerivedTasks.isEmpty()) {
            dto.setCompletionPercentage(0);
        } else {
            long completedCount = scheduledDerivedTasks.stream()
                    .filter(task -> task.getStatus() == ItemStatus.COMPLETE) // Correct enum comparison
                    .count();
            dto.setCompletionPercentage((int) Math.round((double) completedCount * 100.0 / scheduledDerivedTasks.size()));
        }

        return dto;
    }

    /**
     * Calculates the full date range for a month's plan.
     * The range starts on the Monday of the week containing the 1st of the month
     * and ends on the Sunday of the week containing the last day of the month.
     * This ensures the entire month is visible within a grid of complete weeks.
     *
     * @param year  The year
     * @param month The month (1-12)
     * @return Array containing [planningStartDate, planningEndDate]
     */
    private LocalDate[] getMonthDateRange(Integer year, Integer month) {
        // 1. Get the first and last calendar days of the month.
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 2. Calculate the actual start of the planning period.
        // This is the Monday of the week containing the 1st of the month.
        LocalDate planningStartDate = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 3. Calculate the actual end of the planning period.
        // This is the Sunday of the week containing the last day of the month.
        LocalDate planningEndDate = lastDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return new LocalDate[]{planningStartDate, planningEndDate};
    }
}