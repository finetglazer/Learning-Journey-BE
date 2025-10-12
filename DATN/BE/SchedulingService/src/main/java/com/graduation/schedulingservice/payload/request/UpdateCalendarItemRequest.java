package com.graduation.schedulingservice.payload.request;

import lombok.Data;

@Data
public class UpdateCalendarItemRequest {
    private String name;
    private String note;
    private TimeSlotDTO timeSlot;
    private String color;
    private String status; // "INCOMPLETE", "COMPLETE"

    // Type-specific details (only one will be populated based on type)
    private TaskDetailsDTO taskDetails;
    private EventDetailsDTO eventDetails;
    private RoutineDetailsDTO routineDetails;
}