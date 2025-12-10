package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocVersionDTO {
    private Long versionId;
    private String snapshotRef; // MongoDB snapshot ObjectId
    private Integer versionNumber;
    private Long createdBy;
    private String createdByName; // Denormalized from user service
    private String createdByAvatar;
    private LocalDateTime createdAt;
    private String reason; // AUTO_30MIN, SESSION_END, RESTORED, MANUAL
}
