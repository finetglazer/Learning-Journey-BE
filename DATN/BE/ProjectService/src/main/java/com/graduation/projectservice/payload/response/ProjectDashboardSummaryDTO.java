package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDashboardSummaryDTO {
    private List<DeliverableProgressDTO> deliverableProgress;
    private List<TeammateWorkloadDTO> teammateWorkload;
    private TaskStatsDTO taskStats;
    private TimelineResponseDTO timeline;
    private ActiveRiskSummaryDTO riskSummary;
}