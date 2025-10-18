package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class PreviousTimingDTO {
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> daysOfWeek;
}