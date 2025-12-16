package com.graduation.notificationservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for notification list with pagination.
 * This is the data object nested inside BaseResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {
    private List<NotificationDTO> notifications;
    private PaginationDTO pagination;
}
