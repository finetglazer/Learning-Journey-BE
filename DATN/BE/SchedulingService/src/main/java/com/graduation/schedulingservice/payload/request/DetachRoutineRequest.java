package com.graduation.schedulingservice.payload.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DetachRoutineRequest {
    private LocalDateTime exceptionDate;
    private CreateCalendarItemRequest newDetails; // Details for the new detached item
}
