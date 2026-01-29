package com.graduation.forumservice.service;

import com.graduation.forumservice.payload.request.CreateAnswerRequest;
import com.graduation.forumservice.payload.request.EditAnswerRequest;
import com.graduation.forumservice.payload.response.BaseResponse;

/**
 * Service interface for Answer-related operations.
 */
public interface AnswerService {

    /**
     * Retrieves paginated answers for a specific post.
     */
    BaseResponse<?> getAnswersForPost(Long postId, int page, int limit, String sort);

    /**
     * Submits a new answer and increments the post's answer count.
     */
    BaseResponse<?> submitAnswer(Long userId, Long postId, CreateAnswerRequest request);

    /**
     * Edits an existing answer.
     */
    BaseResponse<?> editAnswer(Long userId, Long answerId, EditAnswerRequest request);

    /**
     * Marks/unmarks a specific answer as the 'Accepted Solution'.
     */
    BaseResponse<?> switchAnswerAcceptStatus(Long userId, Long answerId);

    /**
     * Deletes an answer and its associated comments.
     */
    BaseResponse<?> deleteAnswer(Long userId, Long answerId);

    /**
     * Processes an upvote (1) or downvote (-1) for an answer.
     */
    BaseResponse<?> voteAnswer(Long userId, Long answerId, Integer voteType);
}
