package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateSavePostStatusRequest {

    @NotBlank(message = "Target is required")
    @Pattern(regexp = "PRIVATE|PROJECT", message = "Target must be PRIVATE or PROJECT")
    private String target;

    private Long projectId; // Required if target is PROJECT

    private boolean wannaSave;
}