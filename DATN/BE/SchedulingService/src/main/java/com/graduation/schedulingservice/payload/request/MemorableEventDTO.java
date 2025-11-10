package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorableEventDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Day is required")
    @Min(value = 1, message = "Day must be between 1 and 31")
    @Max(value = 31, message = "Day must be between 1 and 31")
    private Integer day;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;
}