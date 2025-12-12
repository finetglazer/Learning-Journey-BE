package com.graduation.documentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doc_snapshots")
public class DocSnapshot {
    @Id
    private String id;

    private String pageId; // StorageRef
    private Long pgNodeId;

    private Map<String, Object> contentSnapshot;
    private List<CommentThread> threadsSnapshot;

    private Integer versionAtSnapshot;

    // ✅ NEW FIELDS FOR HISTORY UI
    private String reason; // e.g., "AUTO_30MIN", "SESSION_END"
    private Long createdBy;      // User ID
    private String createdByName; // Cached User Name
    private String createdByAvatar; // Cached Avatar URL

    private LocalDateTime createdAt;

    public String getIdAsString() {
        return id;
    }
}