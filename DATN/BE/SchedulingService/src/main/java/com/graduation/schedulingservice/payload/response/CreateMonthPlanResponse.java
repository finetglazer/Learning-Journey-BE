package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateMonthPlanResponse {
    private Long monthPlanId;
    private List<String> approvedRoutines;
    private List<Long> weekPlanIds;
    private String message;
}