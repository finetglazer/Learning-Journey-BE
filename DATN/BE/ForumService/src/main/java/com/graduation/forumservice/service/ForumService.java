package com.graduation.forumservice.service;

import com.graduation.forumservice.payload.request.CreateAnswerRequest;
import com.graduation.forumservice.payload.request.CreateCommentRequest;
import com.graduation.forumservice.payload.request.CreatePostRequest;
import com.graduation.forumservice.payload.request.UpdateSavePostStatusRequest;
import com.graduation.forumservice.payload.response.BaseResponse;

public interface ForumService {

    // --- 1. FORUM FEED & SEARCH ---

    /**
     * Retrieves a paginated feed of posts based on filters and search keywords.
     */
    BaseResponse<?> getPostFeed(Long userId, int page, int limit, String filter, String sort, String search);

    /**
     * Handles the logic for creating a new post, including syncing tags.
     */
    BaseResponse<?> createNewPost(Long userId, CreatePostRequest request);


    // --- 2. POST DETAIL & ACTIONS ---

    /**
     * Fetches full post details, including author info and community stats.
     */
    BaseResponse<?> getPostDetail(Long userId, Long postId);

    /**
     * Processes an upvote (1) or downvote (-1) for a post.
     */
    BaseResponse<?> votePost(Long userId, Long postId, Integer voteType);

    /**
     * Toggles the 'solved' status of a post (Post owner only).
     */
    BaseResponse<?> updateSolveStatus(Long userId, Long postId, Boolean isSolved);

    /**
     * Implements strict deletion logic: delete if no answers, otherwise block.
     */
    BaseResponse<?> deletePostStrict(Long userId, Long postId);

    /**
     * Saves a post to a private collection or a specific project space.
     */
    BaseResponse<?> updateSavePostStatus(Long userId, Long postId, UpdateSavePostStatusRequest request);


    // --- 3. ANSWERS MANAGEMENT ---

    /**
     * Retrieves paginated answers for a specific post.
     */
    BaseResponse<?> getAnswersForPost(Long postId, int page, int limit, String sort);

    /**
     * Submits a new answer and increments the post's answer count.
     */
    BaseResponse<?> submitAnswer(Long userId, Long postId, CreateAnswerRequest request);

    /**
     * Marks a specific answer as the 'Accepted Solution'.
     */
    BaseResponse<?> acceptAnswer(Long userId, Long answerId);

    /**
     * Deletes an answer and its associated comments.
     */
    BaseResponse<?> deleteAnswer(Long userId, Long answerId);

    BaseResponse<?> voteAnswer(Long userId, Long answerId, Integer voteType);

    // --- 4. COMMENTS MANAGEMENT ---

    /**
     * Retrieves a flat list of comments for either a Post or an Answer.
     */
    BaseResponse<?> getComments(String targetType, Long targetId, int page, int limit);

    /**
     * Adds a comment or a reply to a specific thread.
     */
    BaseResponse<?> addComment(Long userId, CreateCommentRequest request);

    /**
     * Deletes a comment and updates any reply previews.
     */
    BaseResponse<?> deleteComment(Long userId, Long commentId);

    /**
     * Performs a partial search in the forum_tags dictionary.
     */
    BaseResponse<?> searchTags(String query);
}