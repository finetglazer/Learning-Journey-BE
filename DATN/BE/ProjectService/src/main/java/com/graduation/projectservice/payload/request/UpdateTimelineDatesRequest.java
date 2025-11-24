package com.graduation.projectservice.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTimelineDatesRequest {

    @NotNull(message = "Type is required")
    private ReorderType type; // Ensure ReorderType (TASK, PHASE, DELIVERABLE) is accessible here

    @NotNull(message = "ID is required")
    private Long id;

    @NotNull(message = "Start date is required")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonProperty("end_date")
    private LocalDate endDate;
}