package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemType;
import com.graduation.schedulingservice.model.enums.PlanStatus;
import com.graduation.schedulingservice.payload.request.CreateMonthPlanRequest;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.*;
import com.graduation.schedulingservice.service.MonthPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthPlanServiceImpl implements MonthPlanService {

    private final MonthPlanRepository monthPlanRepository;
    private final WeekPlanRepository weekPlanRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final BigTaskRepository bigTaskRepository;

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

            // 2. Build response
            MonthPlanResponse response = new MonthPlanResponse();
            response.setId(monthPlan.getId());
            response.setYear(monthPlan.getYear());
            response.setMonth(monthPlan.getMonth());
            response.setStatus(monthPlan.getStatus().name());
            response.setApprovedRoutineNames(new ArrayList<>(monthPlan.getApprovedRoutineNames()));

            // 3. Get big tasks with derived tasks count and completion percentage
            List<BigTaskDTO> bigTaskDTOs = monthPlan.getBigTasks().stream()
                    .map(bigTask -> {
                        BigTaskDTO dto = new BigTaskDTO();
                        dto.setId(bigTask.getId());
                        dto.setName(bigTask.getName());
                        dto.setEstimatedStartDate(bigTask.getEstimatedStartDate());
                        dto.setEstimatedEndDate(bigTask.getEstimatedEndDate());

                        // Count derived tasks (Tasks with parentBigTaskId = bigTask.id)
                        List<CalendarItem> derivedTasks = calendarItemRepository.findAllByUserId(userId).stream()
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
            List<EventDTO> eventDTOs = calendarItemRepository.findAllByUserId(userId).stream()
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
     * Creates full Monday-to-Sunday week plans for a given month.
     * The first week starts on the Monday of the week containing the 1st of the month.
     * The last week created is the last full week whose Sunday is within the month.
     *
     * @param monthPlanId The ID of the parent month plan.
     * @param year The target year.
     * @param month The target month.
     * @return A list of IDs for the newly created WeekPlan entities.
     */
    private List<Long> createWeekPlans(Long monthPlanId, Integer year, Integer month) {
        List<Long> weekPlanIds = new ArrayList<>();
        int weekNumber = 1;

        // 1. Define the boundaries of the month.
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 2. Determine the start of the very first week.
        // This is the Monday of the week containing the first day of the month.
        // It might be in the previous month, e.g., for Aug 1, 2025 (a Friday), this will be July 28.
        LocalDate currentWeekStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 3. Loop to create full weeks (Monday to Sunday).
        while (true) {
            // The end of the current week is always 6 days after the start (Sunday).
            LocalDate currentWeekEnd = currentWeekStart.plusDays(6);

            // 4. Stop if the end of this week falls outside the target month.
            // This fulfills your "last Sunday must be in the month range" requirement.
            if (currentWeekEnd.isAfter(lastDayOfMonth)) {
                break;
            }

            // 5. Create and save the WeekPlan entity.
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

            // 6. Move to the start of the next week and increment the counter.
            currentWeekStart = currentWeekStart.plusWeeks(1);
            weekNumber++;
        }

        return weekPlanIds;
    }
}