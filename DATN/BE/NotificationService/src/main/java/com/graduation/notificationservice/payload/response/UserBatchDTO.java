package com.graduation.notificationservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBatchDTO {
    private Long userId;
    private String name;
    private String email;
    private String avatarUrl;
}
