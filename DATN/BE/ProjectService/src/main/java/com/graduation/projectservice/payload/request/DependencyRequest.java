package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DependencyRequest {
    @NotNull
    private String type; // TASK, PHASE, etc.

    @NotNull
    private Long fromId;

    @NotNull
    private Long toId;
}