package com.graduation.userservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimezoneConversionRequest {
    private String oldTimezone;
    private String newTimezone;
}