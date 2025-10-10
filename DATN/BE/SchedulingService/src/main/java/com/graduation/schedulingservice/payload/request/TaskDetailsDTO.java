package com.graduation.schedulingservice.payload.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskDetailsDTO {
    private Integer estimatedHours;
    private LocalDate dueDate;
}