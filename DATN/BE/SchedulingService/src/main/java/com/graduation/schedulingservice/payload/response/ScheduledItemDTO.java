// src/main/java/com/graduation/schedulingservice/payload/dto/ScheduledItemDTO.java
package com.graduation.schedulingservice.payload.response;

import com.graduation.schedulingservice.model.RecurringPattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScheduledItemDTO {
    private Long id;
    private String type;
    private String name;
    private TimeSlotResponseDTO timeSlot;
    private String color;
    private String status;
    private RecurringPattern pattern;
    private java.util.List<java.time.LocalDateTime> exceptions;
    private java.time.LocalDateTime endDate;

    // Task-specific fields for sub-task identification
    private Long parentBigTaskId;
    private String parentBigTaskName;
}
