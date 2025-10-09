package com.graduation.userservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLimitsResponse {
    // ADDED: The global enabled flag for the feature
    private Boolean enabled;

    private Map<String, DailyLimitDto> limits;
}