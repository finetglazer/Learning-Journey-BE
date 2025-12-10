package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduation.projectservice.model.enums.NodeType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FileNodeResponseDTO {
    @JsonProperty("node_id")
    private Long nodeId;

    private String name;

    private NodeType type;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("created_by")
    private String createdBy; // Now the User's Name, not ID

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("size_bytes")
    private Long sizeBytes;

    private String extension;
}