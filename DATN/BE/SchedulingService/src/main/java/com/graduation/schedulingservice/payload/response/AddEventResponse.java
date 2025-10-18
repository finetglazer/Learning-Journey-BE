package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddEventResponse {

    private Long eventId;
    private Long calendarItemId; // Same as eventId
    private String message;
    private Long weekPlanId;
}
