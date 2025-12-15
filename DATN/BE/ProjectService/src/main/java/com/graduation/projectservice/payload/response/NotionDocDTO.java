package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionDocDTO {
    @JsonProperty("node_id")
    private Long nodeId;
    @JsonProperty("project_id")
    private Long projectId; // For file picker
    private String name;
    private String storageReference; // MongoDB ObjectId for WebSocket connection
    private String role; // OWNER or MEMBER - for FE to show/hide delete button
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
