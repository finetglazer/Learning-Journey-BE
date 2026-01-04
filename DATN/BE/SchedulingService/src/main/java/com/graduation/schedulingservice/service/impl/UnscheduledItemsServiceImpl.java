package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.BigTaskRepository;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.MonthPlanRepository;
import com.graduation.schedulingservice.service.UnscheduledItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnscheduledItemsServiceImpl implements UnscheduledItemsService {

    private final MonthPlanRepository monthPlanRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final BigTaskRepository bigTaskRepository;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getUnscheduledItemsGroupedByMonth(Long userId) {
        try {
            // 1. Calculate 6-month window
            LocalDate currentDate = LocalDate.now();
            int currentYear = currentDate.getYear();
            int currentMonth = currentDate.getMonthValue();

            List<MonthGroupDTO> monthGroups = new ArrayList<>();

            // 2. Query all month plans within 6-month window
            for (int i = 0; i < 6; i++) {
                int targetMonth = currentMonth + i;
                int targetYear = currentYear;

                // Handle year overflow
                while (targetMonth > 12) {
                    targetMonth -= 12;
                    targetYear++;
                }

                // 3. Find month plan for this period
                Optional<MonthPlan> monthPlanOpt = monthPlanRepository
                        .findByUserIdAndYearAndMonth(userId, targetYear, targetMonth);

                if (monthPlanOpt.isEmpty()) {
                    log.info("No month plan found for userId={}, year={}, month={}",
                            userId, targetYear, targetMonth);
                    continue;
                }

                MonthPlan monthPlan = monthPlanOpt.get();

                // 4. Find unscheduled routines
                List<UnscheduledRoutineDTO> unscheduledRoutines =
                        findUnscheduledRoutines(userId, monthPlan, targetYear, targetMonth);

                // 5. Find unscheduled tasks
                List<UnscheduledTaskDTO> unscheduledTasks =
                        findUnscheduledTasks(userId, monthPlan);

                // 6. Create month group
                MonthGroupDTO monthGroup = new MonthGroupDTO();
                monthGroup.setMonthPlanId(monthPlan.getId());
                monthGroup.setYear(targetYear);
                monthGroup.setMonth(targetMonth);
                monthGroup.setUnscheduledRoutines(unscheduledRoutines);
                monthGroup.setUnscheduledTasks(unscheduledTasks);

                monthGroups.add(monthGroup);
            }

            // 7. Build response
            UnscheduledItemsGroupedResponse response = new UnscheduledItemsGroupedResponse();
            response.setMonthGroups(monthGroups);

            log.info("Retrieved unscheduled items for {} months for userId={}",
                    monthGroups.size(), userId);

            return new BaseResponse<>(1, "Unscheduled items retrieved successfully", response);

        } catch (Exception e) {
            log.error("Failed to get unscheduled items for userId={}", userId, e);
            return new BaseResponse<>(0, "Failed to retrieve unscheduled items", null);
        }
    }

    /**
     * Find unscheduled routines for a month plan
     */
    private List<UnscheduledRoutineDTO> findUnscheduledRoutines(
            Long userId, MonthPlan monthPlan, int year, int month) {

        List<UnscheduledRoutineDTO> unscheduledRoutines = new ArrayList<>();

        // Get approved routine names from month plan
        Set<String> approvedRoutineNames = new HashSet<>(monthPlan.getApprovedRoutineNames());

        if (approvedRoutineNames.isEmpty()) {
            return unscheduledRoutines;
        }

        // Find all scheduled routines in this month plan
        List<CalendarItem> allItems = calendarItemRepository.findAllByUserId(userId);

        Set<String> scheduledRoutineNames = allItems.stream()
                .filter(item -> item instanceof Routine)
                .filter(item -> item.getMonthPlanId() != null)
                .filter(item -> item.getMonthPlanId().equals(monthPlan.getId()))
                .filter(item -> item.getTimeSlot() != null && item.getTimeSlot().getStartTime() != null)
                .map(CalendarItem::getName)
                .collect(Collectors.toSet());

        List<CalendarItem> unscheduledItems = allItems.stream()
                .filter(item -> item instanceof Routine)
                .filter(item -> item.getMonthPlanId() != null)
                .filter(item -> item.getMonthPlanId().equals(monthPlan.getId()))
                .filter(item -> item.getTimeSlot() == null || item.getTimeSlot().getStartTime() == null)
                .filter(item -> !scheduledRoutineNames.contains(item.getName()))
                .toList();

        // For each unscheduled routine, query previous month's timing
        for (CalendarItem unscheduledItem : unscheduledItems) {
            UnscheduledRoutineDTO dto = new UnscheduledRoutineDTO();
            dto.setId(unscheduledItem.getId());
            dto.setName(unscheduledItem.getName());
            dto.setSource("MONTH_PLAN");
            dto.setNeedsScheduling(true);

            // Query previous month for timing
            PreviousTimingDTO previousTiming = getPreviousRoutineTiming(userId, unscheduledItem.getName(), year, month);
            dto.setPreviousTiming(previousTiming);
            dto.setCanUsePreviousTiming(previousTiming != null);

            unscheduledRoutines.add(dto);
        }

        return unscheduledRoutines;
    }

    /**
     * Get previous month's timing for a routine
     */
    private PreviousTimingDTO getPreviousRoutineTiming(
            Long userId, String routineName, int currentYear, int currentMonth) {

        // Calculate previous month
        int prevMonth = currentMonth - 1;
        int prevYear = currentYear;

        if (prevMonth == 0) {
            prevMonth = 12;
            prevYear = currentYear - 1;
        }

        // Find previous month plan
        Optional<MonthPlan> prevMonthPlanOpt = monthPlanRepository
                .findByUserIdAndYearAndMonth(userId, prevYear, prevMonth);

        if (prevMonthPlanOpt.isEmpty()) {
            return null;
        }

        // Find routine in previous month
        List<CalendarItem> allItems = calendarItemRepository.findAllByUserId(userId);

        Optional<CalendarItem> prevRoutineOpt = allItems.stream()
                .filter(item -> item instanceof Routine)
                .filter(item -> item.getName().equals(routineName))
                .filter(item -> item.getMonthPlanId() != null)
                .filter(item -> item.getMonthPlanId().equals(prevMonthPlanOpt.get().getId()))
                .filter(item -> item.getTimeSlot() != null && item.getTimeSlot().getStartTime() != null)
                .findFirst();

        if (prevRoutineOpt.isEmpty()) {
            return null;
        }

        Routine prevRoutine = (Routine) prevRoutineOpt.get();
        TimeSlot timeSlot = prevRoutine.getTimeSlot();
        RecurringPattern pattern = prevRoutine.getPattern();

        if (timeSlot == null || pattern == null) {
            return null;
        }

        // Build previous timing DTO
        PreviousTimingDTO dto = new PreviousTimingDTO();
        dto.setStartTime(timeSlot.getStartTime().toLocalTime());
        dto.setEndTime(timeSlot.getEndTime().toLocalTime());

        List<String> daysOfWeek = pattern.getDaysOfWeek().stream()
                .map(DayOfWeek::name)
                .collect(Collectors.toList());
        dto.setDaysOfWeek(daysOfWeek);

        return dto;
    }

    /**
     * Find unscheduled tasks for a month plan
     */
    private List<UnscheduledTaskDTO> findUnscheduledTasks(Long userId, MonthPlan monthPlan) {
        List<UnscheduledTaskDTO> unscheduledTasks = new ArrayList<>();

        // 1. Find all big tasks for this month plan
        List<BigTask> bigTasks = bigTaskRepository.findByMonthPlan_Id(monthPlan.getId());

        // Create a map of bigTaskId -> BigTask for quick lookup
        Map<Long, BigTask> bigTaskMap = bigTasks.stream()
                .collect(Collectors.toMap(BigTask::getId, bt -> bt));

        // 2. Find all unscheduled tasks (derived from big tasks)
        List<Task> unscheduledDerivedTasks = calendarItemRepository.findUnscheduledByUserId(userId)
                .stream()
                .filter(item -> item instanceof Task)
                .map(item -> (Task) item)
                .filter(task -> task.getParentBigTaskId() != null)
                .filter(task -> bigTaskMap.containsKey(task.getParentBigTaskId())) // Only tasks from this month's big tasks
                .collect(Collectors.toList());

        // 3. Group unscheduled tasks by their parent big task ID
        Map<Long, List<Task>> tasksByBigTaskId = unscheduledDerivedTasks.stream()
                .collect(Collectors.groupingBy(Task::getParentBigTaskId));

        // 4. Create DTOs for ALL big tasks in this month plan
        for (BigTask bigTask : bigTasks) {
            List<Task> unscheduledTasksForBigTask = tasksByBigTaskId.getOrDefault(bigTask.getId(), Collections.emptyList());

            UnscheduledTaskDTO dto = new UnscheduledTaskDTO();
            dto.setBigTaskId(bigTask.getId());
            dto.setBigTaskName(bigTask.getName());
            dto.setSource("MONTH_PLAN");
            dto.setEstimatedStartDate(bigTask.getEstimatedStartDate());
            dto.setEstimatedEndDate(bigTask.getEstimatedEndDate());

            // Convert unscheduled tasks to suggested subtasks
            List<SuggestedSubtaskDTO> suggestedSubtasks = unscheduledTasksForBigTask.stream()
                    .map(task -> {
                        SuggestedSubtaskDTO subtask = new SuggestedSubtaskDTO();
                        subtask.setId(task.getId());
                        subtask.setName(task.getName());
                        subtask.setDescription(task.getNote());
                        subtask.setEstimated(task.getEstimatedHours() != null ?
                                task.getEstimatedHours() + "h" : null);
                        return subtask;
                    })
                    .collect(Collectors.toList());

            dto.setSuggestedSubtasks(suggestedSubtasks);
            unscheduledTasks.add(dto);
        }


        // B. Find standalone unscheduled tasks
        // NOTE: In current UI, standalone tasks are created via drag-drop (always scheduled)
        // This code handles edge case if tasks are created via API without timeSlot
        // SKIP FOR NOW - uncomment if needed in future
        /*
        List<Task> standaloneTasks = allItems.stream()
            .filter(item -> item instanceof Task)
            .map(item -> (Task) item)
            .filter(task -> task.getMonthPlanId() != null)
            .filter(task -> task.getMonthPlanId().equals(monthPlan.getId()))
            .filter(task -> task.getParentBigTaskId() == null)
            .filter(task -> task.getTimeSlot() == null || task.getTimeSlot().getStartTime() == null)
            .collect(Collectors.toList());
        for (Task task : standaloneTasks) {
            UnscheduledTaskDTO dto = new UnscheduledTaskDTO();
            dto.setTaskId(task.getId());
            dto.setName(task.getName());
            dto.setSource("CALENDAR");
            dto.setEstimatedHours(task.getEstimatedHours());
            dto.setPriority(task.getStatus() != null ? task.getStatus().name() : null);

            // Convert existing subtasks
            List<SuggestedSubtaskDTO> subtasks = task.getSubtasks().stream()
                    .map(subtask -> {
                        SuggestedSubtaskDTO dto2 = new SuggestedSubtaskDTO();
                        dto2.setName(subtask.getName());
                        dto2.setDescription(subtask.getDescription());
                        return dto2;
                    })
                    .collect(Collectors.toList());

            dto.setSubtasks(subtasks);
            unscheduledTasks.add(dto);
        }

         */

        return unscheduledTasks;

    }
}