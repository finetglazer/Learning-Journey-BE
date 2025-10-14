// src/main/java/com/graduation/schedulingservice/payload/response/UnscheduledTaskDTO.java
package com.graduation.schedulingservice.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Prevents null fields from appearing in JSON
public class UnscheduledTaskDTO {
    // For parent tasks
    private Long bigTaskId;
    private String bigTaskName;

    // For individual tasks
    private Long taskId;
    private String name;

    // Common fields
    private String source; // "MONTH_PLAN" or "CALENDAR"
    private List<SubtaskDTO> subtasks;
}