package com.graduation.projectservice.model;

import com.graduation.projectservice.model.enums.RiskLevel;
import com.graduation.projectservice.model.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "pm_risk")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "assignees") // Prevent infinite loop in HashCode
@ToString(exclude = "assignees")          // Prevent infinite loop in ToString
public class PM_Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "risk_statement", columnDefinition = "TEXT")
    private String riskStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "probability")
    private RiskLevel probability;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact")
    private RiskLevel impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RiskStatus status;

    @Column(name = "mitigation_plan", columnDefinition = "TEXT")
    private String mitigationPlan;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "revised_probability")
    private RiskLevel revisedProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "revised_impact")
    private RiskLevel revisedImpact;

    @OneToMany(mappedBy = "risk", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PM_RiskAssignee> assignees;
}