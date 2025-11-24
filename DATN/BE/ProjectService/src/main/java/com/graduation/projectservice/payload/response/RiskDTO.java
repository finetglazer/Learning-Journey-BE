package com.graduation.projectservice.payload.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RiskDTO {
    private Long id;
    private String key;
    private String risk_statement;
    private String probability; // Return label: "5-Very high"
    private String impact;      // Return label: "5-Very high"
    private String status;
    private Integer risk_score; // Calculated field (Prob * Impact)
    private String risk_degree; // LOW, MEDIUM, HIGH
    private List<AssigneeDTO> assignees;
    private String mitigation_plan;
    private String note;
    private String revised_probability;
    private String revised_impact;
}