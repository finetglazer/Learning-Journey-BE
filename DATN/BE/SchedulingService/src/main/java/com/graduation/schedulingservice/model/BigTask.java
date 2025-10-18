package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "big_tasks")
public class BigTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "month_plan_id", nullable = false)
    private MonthPlan monthPlan;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate estimatedStartDate;
    private LocalDate estimatedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.NOT_STARTED;

    // Derived tasks are Task entities with parentBigTaskId = this.id
    // No direct relationship needed - we'll query via CalendarItemRepository

    public Long getMonthPlanId() {
        return monthPlan != null ? monthPlan.getId() : null;
    }
}