package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.MonthPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthPlanRepository extends JpaRepository<MonthPlan, Long> {

    Optional<MonthPlan> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    Optional<MonthPlan> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
}