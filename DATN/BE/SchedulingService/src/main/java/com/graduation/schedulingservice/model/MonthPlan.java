package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "month_plans")
public class MonthPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @ElementCollection
    @CollectionTable(name = "month_plan_approved_routines", joinColumns = @JoinColumn(name = "month_plan_id"))
    @Column(name = "routine_name")
    private List<String> approvedRoutineNames = new ArrayList<>();

    @OneToMany(mappedBy = "monthPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BigTask> bigTasks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.DRAFT;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public void addBigTask(BigTask bigTask) {
        bigTasks.add(bigTask);
        bigTask.setMonthPlan(this);
    }

    public void removeBigTask(BigTask bigTask) {
        bigTasks.remove(bigTask);
        bigTask.setMonthPlan(null);
    }

    public void approveRoutine(String routineName) {
        if (!approvedRoutineNames.contains(routineName)) {
            approvedRoutineNames.add(routineName);
        }
    }

    public void removeRoutine(String routineName) {
        approvedRoutineNames.remove(routineName);
    }
}