package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatsDTO {

    @JsonProperty("by_status")
    private StatsByStatus byStatus;

    @JsonProperty("by_deadline")
    private StatsByDeadline byDeadline;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatsByStatus {
        private long toDo;
        private long inProgress;
        private long inReview;
        private long done;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatsByDeadline {
        private long completed;
        private long dueSoon;
        private long overdue;
        private long unassigned;
    }
}