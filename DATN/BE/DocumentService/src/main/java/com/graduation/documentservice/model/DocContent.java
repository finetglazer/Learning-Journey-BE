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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doc_contents")
public class DocContent {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Field("pg_node_id")
    private Long pgNodeId;

    @Field("project_id")
    private Long projectId;

    @Field("content")
    private Map<String, Object> content; // Tiptap JSON structure

    @Builder.Default
    @Field("threads")
    private List<CommentThread> threads = new ArrayList<>();

    @Builder.Default
    @Field("version")
    private Integer version = 1;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    public String getIdAsString() {
        return id != null ? id.toHexString() : null;
    }
}
