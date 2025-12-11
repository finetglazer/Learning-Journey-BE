package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pm_doc_version",
        uniqueConstraints = @UniqueConstraint(columnNames = {"node_id", "version_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PM_DocVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "snapshot_ref", nullable = false, length = 50)
    private String snapshotRef; // MongoDB ObjectId of snapshot

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reason", nullable = false, length = 20)
    private String reason; // AUTO_30MIN, SESSION_END, RESTORED, MANUAL
}
