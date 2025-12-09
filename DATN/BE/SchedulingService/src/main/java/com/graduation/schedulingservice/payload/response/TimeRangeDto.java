package com.graduation.schedulingservice.payload.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeRangeDto {

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private String startTime; // Format: "HH:mm" e.g., "22:00"

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    private String endTime;   // Format: "HH:mm" e.g., "06:00"
}