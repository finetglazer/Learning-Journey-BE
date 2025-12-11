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
public class SnapshotDTO {
    private String snapshotId; // MongoDB ObjectId as hex string
    private Long pgNodeId;
    private Map<String, Object> contentSnapshot;
    private List<CommentThread> threadsSnapshot;
    private Integer versionAtSnapshot;
    private LocalDateTime createdAt;
}
