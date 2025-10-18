package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecurringPatternDTO {
    private List<String> daysOfWeek; // ["MONDAY", "WEDNESDAY", "FRIDAY"]
}