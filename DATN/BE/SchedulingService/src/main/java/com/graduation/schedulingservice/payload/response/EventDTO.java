package com.graduation.schedulingservice.payload.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventDTO {
    private Long id;
    private String name;
    private LocalDate specificDate;
    private LocalTime startTime;
    private Boolean isScheduled;
}