package com.graduation.notificationservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual notification in API response.
 * All fields use camelCase following REST API conventions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private SenderDTO sender;
    private String contentMessage;
    private String type;
    private String targetUrl;
    private String invitationStatus;
    private Boolean isRead;
    private String createdAt;
}
