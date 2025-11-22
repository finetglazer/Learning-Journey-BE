package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverableStructureDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("key")
    private String key;

    @JsonProperty("order")
    private Integer order;

    // Thêm trường này để FE mặc định expand cho deliverable item
    @JsonProperty("hasChildContainKeyword")
    private boolean hasChildContainKeyword = false;

    @JsonProperty("phases")
    private List<PhaseDTO> phases;
}