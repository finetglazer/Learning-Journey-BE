package com.graduation.projectservice.payload.response;

import com.graduation.projectservice.model.PM_Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskDetailDTO {
    private PM_Task taskInfo;
    private List<TaskAttachmentDetailDTO> attachments;
    private List<TaskCommentDTO> comments;
}