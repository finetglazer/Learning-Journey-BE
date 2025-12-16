package com.graduation.notificationservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for notification sender information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SenderDTO {
    private Long id;
    private String name;
    private String avatar;
}
