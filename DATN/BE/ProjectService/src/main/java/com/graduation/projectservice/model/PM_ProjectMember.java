package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pm_project_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProjectMemberKey.class)
public class PM_ProjectMember {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ProjectMembershipRole role;

    @Column(name = "custom_role_name")
    private String customRoleName;
}