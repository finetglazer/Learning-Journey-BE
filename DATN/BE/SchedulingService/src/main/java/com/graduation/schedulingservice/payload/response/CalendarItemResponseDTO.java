package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarItemResponseDTO {
    private Long id;
    private String type; // "TASK", "ROUTINE", "EVENT"
    private String name;
    private String note;
    private TimeSlotResponseDTO timeSlot;
    private String color;
    private String status; // "INCOMPLETE", "COMPLETE"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Task-specific fields
    private Long parentBigTaskId;
    private String parentBigTaskName;
    private Integer estimatedHours;
    private Integer actualHours;
    private LocalDate dueDate;
    private List<SubtaskDTO> subtasks;
    private Integer completionPercentage;

    // Routine-specific fields
    private RecurringPatternResponseDTO pattern;
    private List<LocalDateTime> exceptions;

    // Event-specific fields
    private String location;
    private List<String> attendees;
}