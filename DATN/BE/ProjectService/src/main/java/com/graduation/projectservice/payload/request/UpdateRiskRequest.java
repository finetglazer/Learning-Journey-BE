package com.graduation.projectservice.payload.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateRiskRequest {
    private String risk_statement;
    private String probability;
    private String impact;
    private List<Long> assignees;
    private String mitigation_plan;
    private String note;
    private String revised_probability;
    private String revised_impact;
}