package com.graduation.schedulingservice.payload.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLimitDto {

    @NotNull(message = "Hours cannot be null")
    @Min(value = 0, message = "Hours must be greater than or equal to 0")
    @Max(value = 24, message = "Hours must be less than or equal to 24")
    private Integer hours;
}