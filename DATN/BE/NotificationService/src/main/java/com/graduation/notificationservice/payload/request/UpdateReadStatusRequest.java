package com.graduation.notificationservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating notification read status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReadStatusRequest {

    @NotNull(message = "isRead field is required")
    private Boolean isRead;
}
