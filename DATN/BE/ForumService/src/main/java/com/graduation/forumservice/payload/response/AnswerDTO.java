package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDTO {

    private Long answerId;

    /**
     * The rich text content stored in MongoDB.
     * Mapped as a Map to support flexible JSON block structures (Notion-style).
     */
    private Map<String, Object> content;

    /**
     * Resolved author details from the User Service.
     */
    private PostAuthorDTO author;

    /**
     * Statistics from the answer_stats table.
     */
    private Integer score;

    /**
     * Indicates if the post author marked this answer as the correct solution.
     */
    private Boolean isAccepted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}