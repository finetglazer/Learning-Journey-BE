package com.graduation.forumservice.service;

import com.graduation.forumservice.payload.request.CreateCommentRequest;
import com.graduation.forumservice.payload.request.UpdateCommentRequest;
import com.graduation.forumservice.payload.response.BaseResponse;

/**
 * Service interface for Comment-related operations.
 */
public interface CommentService {

    /**
     * Retrieves a flat list of comments for either a Post or an Answer.
     */
    BaseResponse<?> getComments(String targetType, Long targetId, int page, int limit);

    /**
     * Adds a comment or a reply to a specific thread.
     */
    BaseResponse<?> addComment(Long userId, CreateCommentRequest request);

    /**
     * Edits an existing comment.
     */
    BaseResponse<?> editComment(Long userId, Long commentId, UpdateCommentRequest request);

    /**
     * Deletes a comment and updates any reply previews.
     */
    BaseResponse<?> deleteComment(Long userId, Long commentId);
}
