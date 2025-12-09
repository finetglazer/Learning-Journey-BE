package com.graduation.schedulingservice.payload.request;


import com.graduation.schedulingservice.payload.response.DailyLimitDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDailyLimitsRequest {

    // ADDED: The global enabled flag for the feature
    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled;

    @NotNull(message = "Limits map cannot be null")
    @Valid
    private Map<String, DailyLimitDto> limits;
}