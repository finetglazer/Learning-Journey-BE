package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.WeekPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {

    /**
     * Find all week plans for a month plan, ordered by week number
     */
    List<WeekPlan> findByMonthPlanIdOrderByWeekNumberAsc(Long monthPlanId);

    /**
     * Find the week plan that contains a specific date
     * @param monthPlanId The month plan ID
     * @param date The date to check
     * @return The week plan containing this date
     */
    @Query("SELECT wp FROM WeekPlan wp WHERE wp.monthPlanId = :monthPlanId " +
            "AND wp.startDate <= :date AND wp.endDate >= :date")
    Optional<WeekPlan> findByMonthPlanIdAndDateWithin(
            @Param("monthPlanId") Long monthPlanId,
            @Param("date") LocalDate date);

    /**
     * Find all week plans that overlap with a date range
     * Used for calculating affected week plans for BigTask
     * @param monthPlanId The month plan ID
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return List of week plans that have any overlap with the date range
     */
    @Query("SELECT wp FROM WeekPlan wp WHERE wp.monthPlanId = :monthPlanId " +
            "AND wp.startDate <= :endDate AND wp.endDate >= :startDate")
    List<WeekPlan> findByMonthPlanIdAndDateRangeOverlap(
            @Param("monthPlanId") Long monthPlanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}