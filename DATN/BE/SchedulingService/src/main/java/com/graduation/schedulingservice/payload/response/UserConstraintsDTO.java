package com.graduation.schedulingservice.payload.response;


import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserConstraintsDTO {
    private List<TimeRangeDto> sleepHours;
    private Boolean allowOverlapping;
    private Boolean dailyLimitFeatureEnabled;
    private Map<String, Integer> dailyLimits;
}