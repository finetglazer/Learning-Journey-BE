package com.graduation.projectservice.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduation.projectservice.model.enums.TaskPriority;
import com.graduation.projectservice.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(max = 255, message = "Task name must not exceed 255 characters")
    private String name;

    private TaskStatus status;

    private TaskPriority priority;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("assigneeIds")
    private List<Long> assigneeIds;
}