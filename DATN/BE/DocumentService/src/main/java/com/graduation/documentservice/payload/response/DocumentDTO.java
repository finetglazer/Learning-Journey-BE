package com.graduation.documentservice.payload.response;

import com.graduation.documentservice.model.CommentThread;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private String storageReference; // MongoDB ObjectId as hex string
    private Long pgNodeId;
    private Long projectId;
    private Map<String, Object> content;
    private List<CommentThread> threads;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
