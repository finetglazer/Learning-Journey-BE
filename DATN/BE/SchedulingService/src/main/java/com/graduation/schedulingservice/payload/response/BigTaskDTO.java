package com.graduation.schedulingservice.payload.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BigTaskDTO {
    private Long id;
    private String name;
    private LocalDate estimatedStartDate;
    private LocalDate estimatedEndDate;
    private Integer derivedTasksCount;
    private Integer completionPercentage;
}