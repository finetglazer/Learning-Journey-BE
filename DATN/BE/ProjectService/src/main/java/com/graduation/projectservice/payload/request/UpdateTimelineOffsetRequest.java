package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTimelineOffsetRequest {
    @NotNull(message = "Type is required (PHASE or DELIVERABLE)")
    private String type; // PHASE or DELIVERABLE

    @NotNull(message = "ID is required")
    private Long id;

    @NotNull(message = "Offset days is required")
    private Integer offsetDays;
}