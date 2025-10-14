// src/main/java/com/graduation/schedulingservice/payload/response/UnscheduledRoutineDTO.java
package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnscheduledRoutineDTO {
    private Long id;
    private String name;
    private String source; // "MONTH_PLAN" or "CALENDAR"
    private boolean canUseFormerTiming;
}