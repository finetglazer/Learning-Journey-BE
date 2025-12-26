package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight DTO for deliverable skeleton structure.
 * Contains phases but NO task details - only counts.
 * Used for initial structure loading before lazy-loading tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkeletonDeliverableDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("key")
    private String key;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("phases")
    private List<SkeletonPhaseDTO> phases;
}
