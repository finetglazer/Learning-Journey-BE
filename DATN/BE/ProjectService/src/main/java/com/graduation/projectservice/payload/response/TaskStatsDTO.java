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
        @JsonProperty("to_do")
        private long toDo;
        @JsonProperty("in_progress")
        private long inProgress;
        @JsonProperty("in_review")
        private long inReview;
        @JsonProperty("done")
        private long done;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatsByDeadline {
        private long completed;
        @JsonProperty("due_soon")
        private long dueSoon;
        private long overdue;
        private long unassigned;
    }
}