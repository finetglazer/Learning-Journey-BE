package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskErrorDetails {
    private Long parentBigTaskId;
    private String requestedMonth;
    private Long requestedMonthPlanId;
}