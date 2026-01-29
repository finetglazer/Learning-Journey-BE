package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.request.CreateCommentRequest;
import com.graduation.forumservice.payload.request.UpdateCommentRequest;
import com.graduation.forumservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Comment-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/forum/comments - targetType={}, targetId={}", targetType, targetId);
        return ResponseEntity.ok(commentService.getComments(targetType, targetId, page, limit));
    }

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.addComment(userId, request));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> editComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request) {
        log.info("PUT /api/forum/comments/{} - userId={}", commentId, userId);
        return ResponseEntity.ok(commentService.editComment(userId, commentId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId) {
        log.info("DELETE /api/forum/comments/{} - userId={}", commentId, userId);
        return ResponseEntity.ok(commentService.deleteComment(userId, commentId));
    }
}
