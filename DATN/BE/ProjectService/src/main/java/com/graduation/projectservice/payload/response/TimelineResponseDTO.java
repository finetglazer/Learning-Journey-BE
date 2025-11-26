package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimelineResponseDTO {
    private LocalDate projectStartDate;
    private LocalDate currentDate;

    private List<TimelineMilestoneDTO> milestones;
}