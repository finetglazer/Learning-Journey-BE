package com.graduation.schedulingservice.payload.request;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
public class TaskDetailsDTO {
    private Integer estimatedHours;
    private LocalDate dueDate;
    private Long parentBigTaskId; // Optional - can be null if not part of a big task
}