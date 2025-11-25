package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveRiskSummaryDTO {

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("display_count")
    private int displayCount;

    private List<ActiveRiskItemDTO> risks;
}