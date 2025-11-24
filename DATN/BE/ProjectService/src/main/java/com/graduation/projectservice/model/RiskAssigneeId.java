package com.graduation.projectservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssigneeId implements Serializable {
    private Long riskId;
    private Long userId;
}