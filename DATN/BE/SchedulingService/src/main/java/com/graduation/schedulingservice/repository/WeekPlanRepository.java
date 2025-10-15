package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.WeekPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {

    List<WeekPlan> findByMonthPlanIdOrderByWeekNumberAsc(Long monthPlanId);
}