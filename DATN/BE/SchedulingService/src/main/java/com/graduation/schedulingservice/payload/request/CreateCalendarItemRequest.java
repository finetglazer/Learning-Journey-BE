package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCalendarItemRequest {

    @NotNull(message = "Calendar ID is required")
    private Long calendarId;

    private Long monthPlanId; // ← NEW: OPTIONAL - null for standalone, provided when scheduling from sidebar

    @NotBlank(message = "Item type is required")
    private String type; // "TASK", "ROUTINE", "EVENT"

    @NotBlank(message = "Name is required")
    private String name;

    private String note;

    private TimeSlotDTO timeSlot; // Optional - for unscheduled items

    private String color;

    // Type-specific details (only one will be populated based on type)
    private TaskDetailsDTO taskDetails;
    private EventDetailsDTO eventDetails;
    private RoutineDetailsDTO routineDetails;
}