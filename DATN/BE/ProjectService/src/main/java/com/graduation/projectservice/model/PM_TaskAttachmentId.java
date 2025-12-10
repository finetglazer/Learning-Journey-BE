package com.graduation.projectservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_TaskAttachmentId implements Serializable {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "node_id")
    private Long nodeId;
}
