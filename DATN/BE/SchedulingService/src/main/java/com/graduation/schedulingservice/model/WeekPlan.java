package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "week_plans")
public class WeekPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monthPlanId;

    @Column(nullable = false)
    private Integer weekNumber; // 1-5 (week within the month)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.DRAFT;
}