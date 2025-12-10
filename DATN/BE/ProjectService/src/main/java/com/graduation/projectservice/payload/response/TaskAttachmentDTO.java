package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskAttachmentDTO {

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("node_id")
    private Long nodeId;

    @JsonProperty("attached_at")
    private LocalDateTime attachedAt;

    @JsonProperty("detached_at")
    private LocalDateTime detachedAt;

    // Constructor for attach response
    public static TaskAttachmentDTO forAttach(Long taskId, Long nodeId, LocalDateTime attachedAt) {
        TaskAttachmentDTO dto = new TaskAttachmentDTO();
        dto.setTaskId(taskId);
        dto.setNodeId(nodeId);
        dto.setAttachedAt(attachedAt);
        return dto;
    }

    // Constructor for detach response
    public static TaskAttachmentDTO forDetach(Long taskId, Long nodeId, LocalDateTime detachedAt) {
        TaskAttachmentDTO dto = new TaskAttachmentDTO();
        dto.setTaskId(taskId);
        dto.setNodeId(nodeId);
        dto.setDetachedAt(detachedAt);
        return dto;
    }
}
