package com.graduation.forumservice.controller;

import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.BaseResponse;
import com.graduation.forumservice.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
            @RequestPart("request") @Valid CreatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("POST /api/forum/posts - userId={}, title={}, fileCount={}",
                userId, request.getTitle(), (files != null ? files.size() : 0));

        return ResponseEntity.ok(forumService.createNewPost(userId, request, files));
    }

    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestPart("request") @Valid UpdatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("PUT /api/forum/posts/{} - userId={}, title={}", postId, userId, request.getTitle());
        return ResponseEntity.ok(forumService.updatePost(userId, postId, request, files));
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

    @PutMapping("/answers/{answerId}")
    public ResponseEntity<?> editAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid EditAnswerRequest request) {
        return ResponseEntity.ok(forumService.editAnswer(userId, answerId, request));
    }

    @PutMapping("/answers/{answerId}/switch-accept-status")
    public ResponseEntity<?> switchAnswerAcceptStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {
        return ResponseEntity.ok(forumService.switchAnswerAcceptStatus(userId, answerId));
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

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> editComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request) {
        log.info("PUT /api/forum/comments/{} - userId={}", commentId, userId);
        return ResponseEntity.ok(forumService.editComment(userId, commentId, request));
    }

    /**
     * POST /api/forum/attachments/{fileId}/save-to-project
     * Copies a forum attachment into a specific project's file manager.
     */
    @PostMapping("/attachments/{fileId}/save-to-project")
    public ResponseEntity<?> saveAttachmentToProject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long fileId,
            @RequestBody @Valid SaveFileToProjectRequest request) {

        // 1. Ensure the IDs match (path variable vs request body)
        // It's good practice to enforce consistency if the fileId is in the URL
        request.setFileId(fileId);
        request.setUserId(userId);

        log.info("POST /save-to-project - User: {}, File: {}, Project: {}",
                userId, fileId, request.getProjectId());

        // 2. Call the service using the DTO directly
        // This allows the service to use request.getStorageRef() and request.getFileSize()
        // without doing an extra database lookup.
        BaseResponse<?> response = forumService.saveFileToProject(request);

        return ResponseEntity.ok(response);
    }

    // --- 5. UTILITIES ---

    @GetMapping("/tags/search")
    public ResponseEntity<?> searchTags(@RequestParam String query) {
        return ResponseEntity.ok(forumService.searchTags(query));
    }
}