package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pm_deliverable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_Deliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deliverable_id")
    private Long deliverableId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

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

    // Relationship to PM_Phase (will be used when you create PM_Phase later)
    // CascadeType.ALL ensures that deleting a deliverable will delete all its phases
    @OneToMany(mappedBy = "deliverableId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PM_Phase> phases = new ArrayList<>();
}