package com.graduation.schedulingservice.payload.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BatchScheduleRequest {
    private List<ItemToSchedule> items;

    @Data
    @NoArgsConstructor
    public static class ItemToSchedule { // <-- Clear, consistent name
        private Long itemId;
        private TimeSlotDTO timeSlot;
    }
}