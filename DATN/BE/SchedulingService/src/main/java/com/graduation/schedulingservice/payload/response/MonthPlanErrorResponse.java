package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthPlanErrorResponse {
    private Boolean success;
    private String error;
    private String message;
    private Object details;
}