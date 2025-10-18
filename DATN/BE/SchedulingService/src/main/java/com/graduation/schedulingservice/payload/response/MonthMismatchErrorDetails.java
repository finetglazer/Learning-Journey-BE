package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthMismatchErrorDetails {
    private String timeSlotMonth;
    private String monthPlanMonth;
    private Long monthPlanId;
}