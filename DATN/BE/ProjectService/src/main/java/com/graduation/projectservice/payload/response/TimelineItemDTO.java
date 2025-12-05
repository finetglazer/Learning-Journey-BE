package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Hides null fields (e.g., children for phases)
public class TimelineItemDTO {

    @JsonProperty("id")
    private String id; // Format: "del-{id}" or "phase-{id}"

    @JsonProperty("type")
    private String type; // "DELIVERABLE" or "PHASE"

    @JsonProperty("name")
    private String name;

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("children")
    private List<TimelineItemDTO> children;

    @JsonProperty("childrenContainSearchKeyword")
    private boolean childrenContainSearchKeyword;
}