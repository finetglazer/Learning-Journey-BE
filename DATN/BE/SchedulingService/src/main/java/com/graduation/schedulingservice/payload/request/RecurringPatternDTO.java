package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecurringPatternDTO {

    @NotNull(message = "Pattern type is required")
    private String type; // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"

    private Integer frequency = 1;

    private List<String> daysOfWeek; // ["MONDAY", "WEDNESDAY", "FRIDAY"]

    private LocalDate endDate;

    private Integer maxOccurrences;
}