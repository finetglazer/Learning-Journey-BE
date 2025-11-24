package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pm_risk_assignee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RiskAssigneeId.class)
@EqualsAndHashCode(exclude = "risk") // Prevent infinite loop
@ToString(exclude = "risk")          // Prevent infinite loop
public class PM_RiskAssignee {

    @Id
    @Column(name = "risk_id")
    private Long riskId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_id", insertable = false, updatable = false)
    private PM_Risk risk;
}