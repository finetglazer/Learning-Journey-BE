// src/main/java/com/graduation/schedulingservice/payload/dto/DateRangeDTO.java
package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeDTO {
    private LocalDateTime start;
    private LocalDateTime end;
}