package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UnscheduledTaskDTO {
    private Long bigTaskId;
    private String bigTaskName;
    private Long taskId;
    private String name;
    private String source;
    private LocalDate estimatedStartDate;
    private LocalDate estimatedEndDate;
    private Integer estimatedHours;
    private String priority;
    private List<SuggestedSubtaskDTO> suggestedSubtasks;
    private List<SuggestedSubtaskDTO> subtasks;
}