package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddUnscheduledTaskRequest {

    @NotBlank(message = "Task name is required")
    private String name;

    private String note;
}
