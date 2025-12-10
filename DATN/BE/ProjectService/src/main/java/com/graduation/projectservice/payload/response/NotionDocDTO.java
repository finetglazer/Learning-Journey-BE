package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionDocDTO {
    private Long nodeId;
    private String name;
    private String storageReference; // MongoDB ObjectId for WebSocket connection
    private String role; // OWNER or MEMBER - for FE to show/hide delete button
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
