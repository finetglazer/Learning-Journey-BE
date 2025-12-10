package com.graduation.projectservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotionDocRequest {
    private Long parentNodeId; // Null if root
    private String name;
}
