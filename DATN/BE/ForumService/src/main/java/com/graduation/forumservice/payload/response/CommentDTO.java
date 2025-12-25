package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long commentId;

    /**
     * The plain text content from 'content_text' column.
     */
    private String content;

    /**
     * Resolved author details from the User Service.
     */
    private PostAuthorDTO author;

    /**
     * If this is a reply, this holds the ID of the parent comment.
     * Null if it's a top-level comment.
     */
    private Long parentCommentId;

    /**
     * A snippet of the comment being replied to, stored as 'reply_preview_snapshot'.
     * Useful for showing context in deep nesting.
     */
    private String replyPreview;

    /**
     * Optional: Targets for debugging or frontend routing.
     */
    private Long postId;
    private Long answerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}