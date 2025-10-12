package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import java.time.LocalTime;

@Data
public class TimeRangeDTO {
    private LocalTime startTime;
    private LocalTime endTime;
}