package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.request.CreateAnswerRequest;
import com.graduation.forumservice.payload.request.EditAnswerRequest;
import com.graduation.forumservice.payload.request.VoteRequest;
import com.graduation.forumservice.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Answer-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @GetMapping("/posts/{postId}/answers")
    public ResponseEntity<?> getAnswers(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "NEWEST") String sort) {
        log.info("GET /api/forum/posts/{}/answers - page={}", postId, page);
        return ResponseEntity.ok(answerService.getAnswersForPost(postId, page, limit, sort));
    }

    @PostMapping("/posts/{postId}/answers")
    public ResponseEntity<?> submitAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid CreateAnswerRequest request) {
        return ResponseEntity.ok(answerService.submitAnswer(userId, postId, request));
    }

    @PutMapping("/answers/{answerId}")
    public ResponseEntity<?> editAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid EditAnswerRequest request) {
        return ResponseEntity.ok(answerService.editAnswer(userId, answerId, request));
    }

    @PutMapping("/answers/{answerId}/switch-accept-status")
    public ResponseEntity<?> switchAnswerAcceptStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {
        return ResponseEntity.ok(answerService.switchAnswerAcceptStatus(userId, answerId));
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId,
            @RequestBody @Valid VoteRequest request) {
        log.info("POST /api/forum/answers/{}/vote - userId={}", answerId, userId);
        return ResponseEntity.ok(answerService.voteAnswer(userId, answerId, request.getVoteType()));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<?> deleteAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long answerId) {
        log.info("DELETE /api/forum/answers/{} - userId={}", answerId, userId);
        return ResponseEntity.ok(answerService.deleteAnswer(userId, answerId));
    }
}
