package com.graduation.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a user updates their date of birth.
 * SchedulingService consumes this to create/update birthday memorable events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayEvent {

    /**
     * The user ID whose birthday was updated.
     */
    private Long userId;

    /**
     * Day of the month (1-31).
     */
    private Integer day;

    /**
     * Month of the year (1-12).
     */
    private Integer month;

    /**
     * Timestamp when the event was created.
     */
    private LocalDateTime timestamp;
}
