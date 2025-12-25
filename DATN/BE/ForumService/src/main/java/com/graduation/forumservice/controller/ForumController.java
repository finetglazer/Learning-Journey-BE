package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    // --- 1. FORUM FEED & SEARCH ---

    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String search) {
        log.info("GET /api/forum/posts - userId={}, filter={}, sort={}, search={}", userId, filter, sort, search);
        return ResponseEntity.ok(forumService.getPostFeed(userId, page, limit, filter, sort, search));
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreatePostRequest request) {
        log.info("POST /api/forum/posts - userId={}, title={}", userId, request.getTitle());
        return ResponseEntity.ok(forumService.createNewPost(userId, request));
    }

    // --- 2. POST DETAIL & ACTIONS ---

    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> getPostDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("GET /api/forum/posts/{} - userId={}", postId, userId);
        return ResponseEntity.ok(forumService.getPostDetail(userId, postId));
    }

    @PostMapping("/posts/{postId}/vote")
    public ResponseEntity<?> votePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid VoteRequest request) {
        return ResponseEntity.ok(forumService.votePost(userId, postId, request.getVoteType()));
    }

    @PutMapping("/posts/{postId}/solve")
    public ResponseEntity<?> markAsSolved(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(forumService.updateSolveStatus(userId, postId, request.get("isSolved")));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(forumService.deletePostStrict(userId, postId));
    }

    @PutMapping("/posts/{postId}/save")
    public ResponseEntity<?> updateSaveStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid UpdateSavePostStatusRequest request) {
        log.info("PUT /api/forum/posts/{}/save - userId={}", postId, userId);
        return ResponseEntity.ok(forumService.updateSavePostStatus(userId, postId, request));
    }

    // --- 3. ANSWERS MANAGEMENT ---

    @GetMapping("/posts/{postId}/answers")
    public ResponseEntity<?> getAnswers(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "NEWEST") String sort) {
        log.info("GET /api/forum/posts/{}/answers - page={}", postId, page);
        return ResponseEntity.ok(forumService.getAnswersForPost(postId, page, limit, sort));
    }

    @PostMapping("/posts/{postId}/answers")
    public ResponseEntity<?> submitAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid CreateAnswerRequest request) {
        return ResponseEntity.ok(forumService.submitAnswer(userId, postId, request));
    }

    @PutMapping("/answers/{answerId}/accept")
    public ResponseEntity<?> acceptAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {
        return ResponseEntity.ok(forumService.acceptAnswer(userId, answerId));
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid VoteRequest request) {
        log.info("POST /api/forum/answers/{}/vote - userId={}", answerId, userId);
        return ResponseEntity.ok(forumService.voteAnswer(userId, answerId, request.getVoteType()));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<?> deleteAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {
        log.info("DELETE /api/forum/answers/{} - userId={}", answerId, userId);
        return ResponseEntity.ok(forumService.deleteAnswer(userId, answerId));
    }

    // --- 4. COMMENTS MANAGEMENT ---

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/forum/comments - targetType={}, targetId={}", targetType, targetId);
        return ResponseEntity.ok(forumService.getComments(targetType, targetId, page, limit));
    }

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreateCommentRequest request) {
        return ResponseEntity.ok(forumService.addComment(userId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId) {
        log.info("DELETE /api/forum/comments/{} - userId={}", commentId, userId);
        return ResponseEntity.ok(forumService.deleteComment(userId, commentId));
    }

    // --- 5. UTILITIES ---

    @GetMapping("/tags/search")
    public ResponseEntity<?> searchTags(@RequestParam String query) {
        return ResponseEntity.ok(forumService.searchTags(query));
    }
}