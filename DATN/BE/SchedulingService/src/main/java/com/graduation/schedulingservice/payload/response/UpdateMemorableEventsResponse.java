package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemorableEventsResponse {

    private Integer eventsCreated;
    private Integer calendarItemsGenerated;
}