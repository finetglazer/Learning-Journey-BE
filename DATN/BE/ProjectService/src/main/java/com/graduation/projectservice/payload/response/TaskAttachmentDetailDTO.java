package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskAttachmentDetailDTO {
//    private Long nodeId;
    private String fileName;
    private String fileType; // extension or mime type
    private String extension;
    private LocalDateTime attachedAt;
//    private Long attachedBy;
    private Long sizeBytes;
}