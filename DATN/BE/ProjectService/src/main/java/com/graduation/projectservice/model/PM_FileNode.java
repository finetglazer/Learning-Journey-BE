package com.graduation.projectservice.model;

import com.graduation.projectservice.model.enums.NodeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pm_file_node")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_FileNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "parent_node_id")
    private Long parentNodeId; // Null if Root

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NodeType type;

    @Column(name = "extension")
    private String extension;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_reference") // Stores the GCS Blob Name or URL
    private String storageReference;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}