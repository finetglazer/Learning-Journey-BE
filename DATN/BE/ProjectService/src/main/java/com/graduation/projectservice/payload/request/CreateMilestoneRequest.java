package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMilestoneRequest {

    @NotBlank(message = "Milestone name is required")
    private String name;

    @NotNull(message = "Milestone date is required")
    private LocalDate date;
}