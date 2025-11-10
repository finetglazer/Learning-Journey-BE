package com.graduation.projectservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pm_task_assignee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_TaskAssignee {

    @EmbeddedId
    private PM_TaskAssigneeId id;

    @Column(name = "task_id", insertable = false, updatable = false)
    private Long taskId;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    public PM_TaskAssignee(Long taskId, Long userId) {
        this.id = new PM_TaskAssigneeId(taskId, userId);
        this.taskId = taskId;
        this.userId = userId;
    }
}