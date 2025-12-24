package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAuthorDTO {
    private Long userId;    // Matches 'user_id' in forum_posts
    private String name;    // Extracted from User Service/Cache
    private String avatar;  // URL for the user's profile picture
}