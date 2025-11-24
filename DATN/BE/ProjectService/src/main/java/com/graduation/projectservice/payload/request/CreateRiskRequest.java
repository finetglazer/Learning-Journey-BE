package com.graduation.projectservice.payload.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateRiskRequest {
    private String risk_statement;
    private String probability;
    private String impact;
    private List<Long> assigneeIds;
}