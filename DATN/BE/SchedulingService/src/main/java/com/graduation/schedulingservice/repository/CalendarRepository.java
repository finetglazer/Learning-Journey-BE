package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    /**
     * Find all calendars belonging to a user
     */
    List<Calendar> findByUserId(Long userId);

    /**
     * Find a calendar by ID and user ID (for authorization)
     */
    Optional<Calendar> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if a calendar exists for a user
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}