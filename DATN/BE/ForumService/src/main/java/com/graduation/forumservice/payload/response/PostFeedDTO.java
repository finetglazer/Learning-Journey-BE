package com.graduation.forumservice.payload.response;

import com.graduation.forumservice.model.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFeedDTO {
    private Long postId;
    private String title;
    private String preview;

    // Author information (can be expanded to a nested UserDTO later)
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    private List<String> tags; // PostgreSQL text[] mapped via Hibernate 6

    // Statistics from the post_stats table
    private Integer score;
    private Long viewCount;
    private Integer answerCount;
    private Boolean isSolved;

    private LocalDateTime createdAt;
    private PostStatus status;
}