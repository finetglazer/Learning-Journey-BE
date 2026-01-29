package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.BaseResponse;
import com.graduation.forumservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Post-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String search) {
        log.info("GET /api/forum/posts - userId={}, filter={}, sort={}, search={}", userId, filter, sort, search);
        return ResponseEntity.ok(postService.getPostFeed(userId, page, limit, filter, sort, search));
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("request") @Valid CreatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("POST /api/forum/posts - userId={}, title={}, fileCount={}",
                userId, request.getTitle(), (files != null ? files.size() : 0));

        return ResponseEntity.ok(postService.createNewPost(userId, request, files));
    }

    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestPart("request") @Valid UpdatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("PUT /api/forum/posts/{} - userId={}, title={}", postId, userId, request.getTitle());
        return ResponseEntity.ok(postService.updatePost(userId, postId, request, files));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> getPostDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("GET /api/forum/posts/{} - userId={}", postId, userId);
        return ResponseEntity.ok(postService.getPostDetail(userId, postId));
    }

    @PostMapping("/posts/{postId}/vote")
    public ResponseEntity<?> votePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid VoteRequest request) {
        return ResponseEntity.ok(postService.votePost(userId, postId, request.getVoteType()));
    }

    @PutMapping("/posts/{postId}/solve")
    public ResponseEntity<?> markAsSolved(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(postService.updateSolveStatus(userId, postId, request.get("isSolved")));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(postService.deletePostStrict(userId, postId));
    }

    @PutMapping("/posts/{postId}/save")
    public ResponseEntity<?> updateSaveStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid UpdateSavePostStatusRequest request) {
        log.info("PUT /api/forum/posts/{}/save - userId={}", postId, userId);
        return ResponseEntity.ok(postService.updateSavePostStatus(userId, postId, request));
    }

    @PostMapping("/attachments/{fileId}/save-to-project")
    public ResponseEntity<?> saveAttachmentToProject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long fileId,
            @RequestBody @Valid SaveFileToProjectRequest request) {

        request.setFileId(fileId);
        request.setUserId(userId);

        log.info("POST /save-to-project - User: {}, File: {}, Project: {}",
                userId, fileId, request.getProjectId());

        BaseResponse<?> response = postService.saveFileToProject(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tags/search")
    public ResponseEntity<?> searchTags(@RequestParam String query) {
        return ResponseEntity.ok(postService.searchTags(query));
    }
}
