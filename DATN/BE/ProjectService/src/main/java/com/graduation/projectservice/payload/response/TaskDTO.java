package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("phaseId")
    private Long phaseId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("key")
    private String key;

    @JsonProperty("status")
    private String status;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("assignees")
    private List<AssigneeDTO> assignees;

    public TaskDTO(Long id, Long phaseId, String name, String key, String status, String priority, Integer order) {
        this.id = id;
        this.phaseId = phaseId;
        this.name = name;
        this.key = key;
        this.status = status;
        this.priority = priority;
        this.order = order;
    }
}