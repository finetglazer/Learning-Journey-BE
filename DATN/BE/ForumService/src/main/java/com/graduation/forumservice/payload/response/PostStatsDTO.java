package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostStatsDTO {
    private Integer score;       // Upvotes - Downvotes
    private Long viewCount;      // Total visits
    private Integer answerCount; // Total responses
    private Boolean isSolved;    // Status flag from forum_posts
}