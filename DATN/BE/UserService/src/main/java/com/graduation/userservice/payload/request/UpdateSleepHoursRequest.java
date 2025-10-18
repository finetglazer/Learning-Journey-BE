package com.graduation.userservice.payload.request;

import com.graduation.userservice.payload.response.TimeRangeDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSleepHoursRequest {

    @NotNull(message = "Sleep hours list cannot be null")
    @Valid
    private List<TimeRangeDto> sleepHours;
}