package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBigTaskRequest { 

    @NotBlank(message = "Big task name is required")
    private String name;

    private String description;

    @NotNull(message = "Estimated start date is required")
    private LocalDate estimatedStartDate;

    @NotNull(message = "Estimated end date is required")
    private LocalDate estimatedEndDate;
}
