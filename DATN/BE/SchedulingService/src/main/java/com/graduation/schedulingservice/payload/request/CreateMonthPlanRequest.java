package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMonthPlanRequest {

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be at least 2020")
    @Max(value = 2030, message = "Year must be at most 2030")
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;
}