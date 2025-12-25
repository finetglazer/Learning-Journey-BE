package com.graduation.projectservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateEvent {
    private Long taskId;
    private Long projectId;
    private Long updatedBy;
    private Set<Long> assigneeIds;
    private String action;

    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_COMMENT_ADD = "COMMENT_ADD";
    public static final String ACTION_COMMENT_UPDATE = "COMMENT_UPDATE";
    public static final String ACTION_COMMENT_DELETE = "COMMENT_DELETE";
}
