package com.graduation.schedulingservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "subtasks")
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id", nullable = false)
    private Task parentTask;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean isComplete = false;

    private LocalDateTime completedAt;

    public void complete() {
        this.isComplete = true;
        this.completedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.isComplete = false;
        this.completedAt = null;
    }

    public Long getParentTaskId() {
        return parentTask != null ? parentTask.getId() : null;
    }
}