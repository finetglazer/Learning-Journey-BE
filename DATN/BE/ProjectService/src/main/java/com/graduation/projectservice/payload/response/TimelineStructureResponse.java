package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineStructureResponse {

    @JsonProperty("items")
    private List<TimelineItemDTO> items;

    @JsonProperty("milestones")
    private List<TimelineMilestoneDTO> milestones;
}