package com.graduation.projectservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectInvitationEvent {
    private Long recipientId;
    private Long senderId;
    private Long projectId;
    private String projectName;
    private String token;
    private Boolean isAccepted; // True=Accepted, False=Declined, Null=Sent
    private LocalDateTime timestamp;
}
