package com.graduation.userservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SleepHoursResponse {
    private List<TimeRangeDto> sleepHours;
}