package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteTimelineRequest {
    @NotNull(message = "Type is required (PHASE or DELIVERABLE or TASK)")
    private String type; // PHASE or DELIVERABLE or TASK

    @NotNull(message = "ID is required")
    private Long id;
}