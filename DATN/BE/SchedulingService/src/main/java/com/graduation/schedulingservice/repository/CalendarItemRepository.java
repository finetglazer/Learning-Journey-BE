package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.CalendarItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // ADD THIS IMPORT
import java.util.Optional;

@Repository
public interface CalendarItemRepository extends JpaRepository<CalendarItem, Long> {

    Optional<CalendarItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    // ADD THIS METHOD
    List<CalendarItem> findAllByUserId(Long userId);
}