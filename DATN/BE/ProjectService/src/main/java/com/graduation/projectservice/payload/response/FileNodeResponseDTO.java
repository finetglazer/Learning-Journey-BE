package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduation.projectservice.model.enums.NodeType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileNodeResponseDTO {
    private Long nodeId;

    private Long parentNodeId;

    private String name;

    private NodeType type;

    private LocalDateTime updatedAt;

    private String createdBy; // Now the User's Name, not ID

    private String avatarUrl;

    private Long sizeBytes;

    private String extension;

    private String storageReference;
}