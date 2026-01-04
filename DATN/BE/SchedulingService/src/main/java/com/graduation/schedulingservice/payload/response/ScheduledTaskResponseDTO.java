package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskResponseDTO {
    private Long id;
    private String name;
    private String note;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
