package com.graduation.userservice.payload.response;

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
    private Integer hours;

    // REMOVED: The 'enabled' field is now global, not per-item.
    // private Boolean enabled;
}