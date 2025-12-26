package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for phase skeleton structure.
 * Contains only basic phase info and task count - NO task details.
 * Used for initial structure loading before lazy-loading tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkeletonPhaseDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("key")
    private String key;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("deliverableId")
    private Long deliverableId;

    /**
     * Number of tasks in this phase.
     * Used to show "(5 tasks)" without loading actual task data.
     */
    @JsonProperty("taskCount")
    private Integer taskCount;
}
