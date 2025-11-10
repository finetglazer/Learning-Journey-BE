package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhaseRequest {

    @NotBlank(message = "Phase name is required")
    @Size(max = 255, message = "Phase name must not exceed 255 characters")
    private String name;
}