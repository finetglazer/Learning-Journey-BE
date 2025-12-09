package com.graduation.projectservice.payload.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RiskDTO {
    private Long id;
    private String key;
    private String riskStatement;
    private String probability; // Return label: "5-Very high"
    private String impact;      // Return label: "5-Very high"
    private String status;
    private Integer riskScore; // Calculated field (Prob * Impact)
    private String riskDegree; // LOW, MEDIUM, HIGH
    private List<AssigneeDTO> assignees;
    private String mitigationPlan;
    private String note;
    private String revisedProbability;
    private String revisedImpact;
}