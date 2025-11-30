package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("avatarUrl")
    private String avatarUrl;
}