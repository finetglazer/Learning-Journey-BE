package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.CalendarItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarItemRepository extends JpaRepository<CalendarItem, Long> {

    List<CalendarItem> findAllByUserId(Long userId);

    /**
     * Find all scheduled items for a user on a specific date
     * Used for daily limit validation
     */
    @Query("SELECT ci FROM CalendarItem ci WHERE ci.userId = :userId " +
            "AND ci.timeSlot IS NOT NULL " +
            "AND ci.timeSlot.startTime IS NOT NULL " +
            "AND FUNCTION('DATE', ci.timeSlot.startTime) = :date")
    List<CalendarItem> findAllByUserIdAndDate(@Param("userId") Long userId,
                                              @Param("date") LocalDate date);

    /**
     * Find all scheduled items that overlap with the given time range
     *
     * Overlap logic: Two time ranges [A_start, A_end] and [B_start, B_end] overlap if:
     * A_start < B_end AND A_end > B_start
     *
     * This also catches identical time ranges:
     * - For [15:00, 23:00] and [15:00, 23:00]:
     *   15:00 < 23:00 (true) AND 23:00 > 15:00 (true) → OVERLAP DETECTED
     *
     * Edge cases handled:
     * - Exact same times: OVERLAP
     * - One slot entirely within another: OVERLAP
     * - Partial overlap: OVERLAP
     * - Adjacent slots (end = start): NO OVERLAP
     */
    @Query("SELECT ci FROM CalendarItem ci WHERE ci.userId = :userId " +
            "AND ci.timeSlot IS NOT NULL " +
            "AND ci.timeSlot.startTime IS NOT NULL " +
            "AND ci.timeSlot.endTime IS NOT NULL " +
            "AND ci.timeSlot.startTime < :endTime " +
            "AND ci.timeSlot.endTime > :startTime")
    List<CalendarItem> findOverlappingItems(@Param("userId") Long userId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}