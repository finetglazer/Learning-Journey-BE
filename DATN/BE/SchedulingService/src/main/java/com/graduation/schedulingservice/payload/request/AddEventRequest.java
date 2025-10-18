package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddEventRequest {

    @NotNull(message = "Calendar ID is required")
    private Long calendarId;

    @NotBlank(message = "Event name is required")
    private String name;

    private String note;

    @NotNull(message = "Specific date is required")
    private LocalDate specificDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}
