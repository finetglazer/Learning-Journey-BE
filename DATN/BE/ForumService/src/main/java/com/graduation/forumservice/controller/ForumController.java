package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.BaseResponse;
import com.graduation.forumservice.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for forum-related endpoints including posts, answers, and comments.
 */
@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    // --- 1. FORUM FEED & SEARCH ---

    /**
     * Retrieves the main forum feed with filtering, searching, and sorting.
     */
    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String search) {

        log.info("GET /api/forum/posts - userId={}, filter={}, sort={}, search={}", userId, filter, sort, search);

        BaseResponse<?> response = forumService.getPostFeed(userId, page, limit, filter, sort, search);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new forum post.
     */
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreatePostRequest request) {

        log.info("POST /api/forum/posts - userId={}, title={}", userId, request.getTitle());

        BaseResponse<?> response = forumService.createNewPost(userId, request);
        return ResponseEntity.ok(response);
    }

    // --- 2. POST DETAIL & ACTIONS ---

    /**
     * Retrieves full details for a specific post.
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> getPostDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {

        log.info("GET /api/forum/posts/{} - userId={}", postId, userId);

        BaseResponse<?> response = forumService.getPostDetail(userId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Records a vote (upvote/downvote) on a post.
     */
    @PostMapping("/posts/{postId}/vote")
    public ResponseEntity<?> votePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid VoteRequest request) {

        log.info("POST /api/forum/posts/{}/vote - userId={}, type={}", postId, userId, request.getVoteType());

        BaseResponse<?> response = forumService.votePost(userId, postId, request.getVoteType());
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a post as solved.
     */
    @PutMapping("/posts/{postId}/solve")
    public ResponseEntity<?> markAsSolved(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody Map<String, Boolean> request) {

        log.info("PUT /api/forum/posts/{}/solve - userId={}, isSolved={}", postId, userId, request.get("isSolved"));

        BaseResponse<?> response = forumService.updateSolveStatus(userId, postId, request.get("isSolved"));
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a post. Implementation includes strict logic regarding answer counts.
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {

        log.info("DELETE /api/forum/posts/{} - userId={}", postId, userId);

        BaseResponse<?> response = forumService.deletePostStrict(userId, postId);
        return ResponseEntity.ok(response);
    }

    // --- 3. ANSWERS MANAGEMENT ---

    /**
     * Submits a new answer to a post.
     */
    @PostMapping("/posts/{postId}/answers")
    public ResponseEntity<?> submitAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid CreateAnswerRequest request) {

        log.info("POST /api/forum/posts/{}/answers - userId={}", postId, userId);

        BaseResponse<?> response = forumService.submitAnswer(userId, postId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Accepts a specific answer as the correct solution.
     */
    @PutMapping("/answers/{answerId}/accept")
    public ResponseEntity<?> acceptAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {

        log.info("PUT /api/forum/answers/{}/accept - userId={}", answerId, userId);

        BaseResponse<?> response = forumService.acceptAnswer(userId, answerId);
        return ResponseEntity.ok(response);
    }

    // --- 4. COMMENTS MANAGEMENT ---

    /**
     * Adds a comment to a Post or Answer.
     */
    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreateCommentRequest request) {

        log.info("POST /api/forum/comments - userId={}, targetType={}, targetId={}",
                userId, request.getTargetType(), request.getTargetId());

        BaseResponse<?> response = forumService.addComment(userId, request);
        return ResponseEntity.ok(response);
    }

    // --- 5. UTILITIES ---

    /**
     * Searches for existing tags based on a partial query.
     */
    @GetMapping("/tags/search")
    public ResponseEntity<?> searchTags(@RequestParam String query) {
        log.info("GET /api/forum/tags/search - query={}", query);

        BaseResponse<?> response = forumService.searchTags(query);
        return ResponseEntity.ok(response);
    }
}