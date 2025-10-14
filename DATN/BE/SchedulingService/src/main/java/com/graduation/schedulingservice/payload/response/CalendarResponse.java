package com.graduation.schedulingservice.payload.response;

import com.graduation.schedulingservice.model.Calendar;
import com.graduation.schedulingservice.model.enums.CalendarType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarResponse {
    private Long id;
    private String name;
    private CalendarType type;
    private Boolean isVisible;
    private Boolean isPinned;

    public static CalendarResponse from(Calendar calendar) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getName(),
                calendar.getType(),
                calendar.getIsVisible(),
                calendar.getIsPinned()
        );
    }
}