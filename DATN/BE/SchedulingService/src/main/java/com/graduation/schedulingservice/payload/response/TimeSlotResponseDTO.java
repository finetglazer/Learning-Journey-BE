package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponseDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}