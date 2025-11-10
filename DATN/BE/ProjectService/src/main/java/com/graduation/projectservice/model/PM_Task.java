package com.graduation.projectservice.model;

import com.graduation.projectservice.model.enums.TaskPriority;
import com.graduation.projectservice.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pm_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "phase_id", nullable = false)
    private Long phaseId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key", nullable = false)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.TO_DO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TaskPriority priority = TaskPriority.MINOR;

    @Column(name = "\"order\"", nullable = false)
    private Integer order = 0;

    @Column(name = "date_added", nullable = false)
    private LocalDate dateAdded = LocalDate.now();

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "taskId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PM_TaskAssignee> assignees = new HashSet<>();
}