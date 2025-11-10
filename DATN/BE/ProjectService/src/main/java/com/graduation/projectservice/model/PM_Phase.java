package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pm_phase")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_Phase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phase_id")
    private Long phaseId;

    @Column(name = "deliverable_id", nullable = false)
    private Long deliverableId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "\"order\"", nullable = false)
    private Integer order = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "phaseId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PM_Task> tasks = new ArrayList<>();
}