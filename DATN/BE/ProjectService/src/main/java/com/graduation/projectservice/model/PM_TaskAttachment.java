package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pm_task_attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_TaskAttachment {

    @EmbeddedId
    private PM_TaskAttachmentId id;

    @Column(name = "task_id", insertable = false, updatable = false)
    private Long taskId;

    @Column(name = "node_id", insertable = false, updatable = false)
    private Long nodeId;

    @Column(name = "added_by_user_id", nullable = false)
    private Long addedByUserId;

    @CreationTimestamp
    @Column(name = "attached_at", updatable = false)
    private LocalDateTime attachedAt;

    public PM_TaskAttachment(Long taskId, Long nodeId, Long addedByUserId) {
        this.id = new PM_TaskAttachmentId(taskId, nodeId);
        this.taskId = taskId;
        this.nodeId = nodeId;
        this.addedByUserId = addedByUserId;
    }
}
