package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutineErrorDetails {
    private String itemName;
    private String itemType;
    private String requestedMonth;
    private Long requestedMonthPlanId;
}