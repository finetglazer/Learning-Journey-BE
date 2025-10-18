package com.graduation.schedulingservice.payload.response;

import lombok.Data;

import java.util.List;

@Data
public class MonthPlanResponse {
    private Long id;
    private Integer year;
    private Integer month;
    private String status;
    private List<String> approvedRoutineNames;
    private List<BigTaskDTO> bigTasks;
    private List<EventDTO> events;
    private List<WeekPlanDTO> weekPlans;
}