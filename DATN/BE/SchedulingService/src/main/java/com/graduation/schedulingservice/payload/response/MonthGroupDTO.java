package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import java.util.List;

@Data
public class MonthGroupDTO {
    private Long monthPlanId;
    private Integer year;
    private Integer month;
    private List<UnscheduledRoutineDTO> unscheduledRoutines;
    private List<UnscheduledTaskDTO> unscheduledTasks;
}