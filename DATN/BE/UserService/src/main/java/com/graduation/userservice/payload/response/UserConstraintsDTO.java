package com.graduation.userservice.payload.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserConstraintsDTO {
    private List<DailyLimitsResponse.TimeRangeDTO> sleepHours;
    private Boolean allowOverlapping;
    private Boolean dailyLimitFeatureEnabled;
    private Map<String, Integer> dailyLimits;
}