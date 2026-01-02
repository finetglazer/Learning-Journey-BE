package com.graduation.forumservice.payload.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PostDetailDTO {
    private Long postId;
    private String title;
    private Map<String, Object> content; // Full Notion-style JSON
    private PostAuthorDTO author;
    private List<String> tags; // PostgresSQL text[]
    private PostStatsDTO stats;
    private List<AnswerDTO> answers;
    private List<CommentDTO> comments;
    private Integer userVote; // Specific to current user
    private Boolean isSaved; // Specific to current user
    private List<Long> savedToProjectIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}