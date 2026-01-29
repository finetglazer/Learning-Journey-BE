package com.graduation.forumservice.service;

import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for Post-related operations.
 */
public interface PostService {

    /**
     * Retrieves a paginated feed of posts based on filters and search keywords.
     */
    BaseResponse<?> getPostFeed(Long userId, int page, int limit, String filter, String sort, String search);

    /**
     * Creates a new post with optional file attachments.
     */
    BaseResponse<?> createNewPost(Long userId, CreatePostRequest request, List<MultipartFile> files);

    /**
     * Updates an existing post with optional file management.
     */
    BaseResponse<?> updatePost(Long userId, Long postId, UpdatePostRequest request, List<MultipartFile> files);

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

    /**
     * Searches for tags matching the query.
     */
    BaseResponse<?> searchTags(String query);

    /**
     * Saves a forum file attachment to a project.
     */
    BaseResponse<?> saveFileToProject(SaveFileToProjectRequest request);

    /**
     * Internal: Retrieves all forum posts linked/shared to a specific project.
     */
    BaseResponse<?> getSharedPostsByProject(Long projectId);
}
