package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveRiskSummaryDTO {
    private int totalCount;
    private int displayCount;

    private List<ActiveRiskItemDTO> risks;
}