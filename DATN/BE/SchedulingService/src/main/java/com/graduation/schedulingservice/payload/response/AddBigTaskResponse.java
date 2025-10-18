package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBigTaskResponse {

    private Long bigTaskId;
    private String message;
    private List<Long> affectedWeekPlanIds;
}
