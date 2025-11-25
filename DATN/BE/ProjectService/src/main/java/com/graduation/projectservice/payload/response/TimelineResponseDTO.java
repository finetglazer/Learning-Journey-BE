package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimelineResponseDTO {

    @JsonProperty("project_start_date")
    private LocalDate projectStartDate;

    @JsonProperty("current_date")
    private LocalDate currentDate;

    private List<TimelineMilestoneDTO> milestones;
}