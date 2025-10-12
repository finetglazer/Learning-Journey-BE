package com.graduation.schedulingservice.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TimeRange {

    private LocalTime startTime;
    private LocalTime endTime;

    public boolean contains(LocalTime time) {
        // Handle overnight ranges (e.g., 22:00 - 06:00)
        if (startTime.isAfter(endTime)) {
            return time.isAfter(startTime) || time.isBefore(endTime);
        }
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public boolean overlaps(TimeRange other) {
        return this.contains(other.startTime) ||
                this.contains(other.endTime) ||
                other.contains(this.startTime);
    }

    public long durationMinutes() {
        if (startTime.isAfter(endTime)) {
            // Overnight range
            return (24 * 60) - startTime.toSecondOfDay() / 60 + endTime.toSecondOfDay() / 60;
        }
        return (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 60;
    }
}