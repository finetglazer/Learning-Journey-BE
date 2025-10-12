package com.graduation.schedulingservice.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
//    private String timezone;

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    public boolean overlaps(TimeSlot other) {
        if (other == null || startTime == null || endTime == null
                || other.startTime == null || other.endTime == null) {
            return false;
        }
        return startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime);
    }

    public boolean contains(LocalDateTime dateTime) {
        if (dateTime == null || startTime == null || endTime == null) {
            return false;
        }
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }
}