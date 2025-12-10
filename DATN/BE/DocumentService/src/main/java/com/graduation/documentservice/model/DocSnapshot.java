package com.graduation.documentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    private ObjectId id;

    @Indexed
    @Field("page_id")
    private ObjectId pageId; // Reference to doc_contents._id

    @Indexed
    @Field("pg_node_id")
    private Long pgNodeId;

    @Field("content_snapshot")
    private Map<String, Object> contentSnapshot;

    @Field("threads_snapshot")
    private List<CommentThread> threadsSnapshot;

    @Field("version_at_snapshot")
    private Integer versionAtSnapshot;

    @Indexed
    @Field("created_at")
    private LocalDateTime createdAt;

    public String getIdAsString() {
        return id != null ? id.toHexString() : null;
    }
}
