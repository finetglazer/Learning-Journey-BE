package com.graduation.schedulingservice.payload.response;

import lombok.Data;

@Data
public class WeekPlanDTO {
    private Integer weekNumber;
    private Long weekPlanId;
    private String status;
}