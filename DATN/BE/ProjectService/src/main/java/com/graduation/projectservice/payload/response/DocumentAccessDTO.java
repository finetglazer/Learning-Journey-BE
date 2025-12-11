package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccessDTO {
    private Long nodeId;
    private Long projectId;
    private String role; // OWNER or MEMBER
    private Boolean canEdit;
    private Boolean canDelete;
    
    // User info for presence feature
    private Long userId;
    private String userName;
    private String userAvatar;
}
