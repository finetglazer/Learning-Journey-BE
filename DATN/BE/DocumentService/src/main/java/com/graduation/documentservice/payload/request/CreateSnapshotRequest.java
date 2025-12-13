package com.graduation.documentservice.payload.request;

import lombok.Data;

@Data
public class CreateSnapshotRequest {
    private String reason;
    private Long createdBy;

    // ✅ Add these fields to capture user info from Hocuspocus
    private String createdByName;
    private String createdByAvatar;
}