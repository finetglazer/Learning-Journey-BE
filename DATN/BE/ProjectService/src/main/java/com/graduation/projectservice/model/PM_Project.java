package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "pm_project")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "task_counter", nullable = false)
    private Long taskCounter = 0L;

    @Column(name = "risk_counter", nullable = false)
    private Long riskCounter = 0L;

    @Column(name = "deliverable_counter", nullable = false)
    private Long deliverableCounter = 0L;

    @Column(name = "phase_counter", nullable = false)
    private Long phaseCounter = 0L;
}