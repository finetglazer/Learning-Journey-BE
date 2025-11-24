package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pm_dependency",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"project_id", "type", "source_id", "target_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // TASK, PHASE, or DELIVERABLE
    @Column(name = "type", nullable = false)
    private String type;

    // The ID of the item the line starts from
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    // The ID of the item the line goes to
    @Column(name = "target_id", nullable = false)
    private Long targetId;
}