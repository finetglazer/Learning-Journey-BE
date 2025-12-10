package com.graduation.documentservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSnapshotRequest {
    private String reason; // AUTO_30MIN, SESSION_END, RESTORED, MANUAL
    private Long createdBy;
}
