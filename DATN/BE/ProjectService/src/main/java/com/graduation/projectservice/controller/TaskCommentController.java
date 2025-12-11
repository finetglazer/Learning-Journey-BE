package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.request.CreateCommentRequest;
import com.graduation.projectservice.payload.request.UpdateCommentRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.TaskCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    /**
     * GET /api/pm/tasks/{taskId}/comments
     * Retrieve all comments for a task
     */
    @GetMapping("/api/pm/tasks/{taskId}/comments")
    public BaseResponse<?> getComments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long taskId
    ) {
        return taskCommentService.getComments(userId, taskId);
    }

    /**
     * POST /api/pm/tasks/{taskId}/comments
     * Add a new comment to a task
     */
    @PostMapping("/api/pm/tasks/{taskId}/comments")
    public BaseResponse<?> createComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long taskId,
            @RequestBody CreateCommentRequest request
    ) {
        return taskCommentService.createComment(userId, taskId, request);
    }

    /**
     * PUT /api/pm/comments/{commentId}
     * Edit an existing comment (creator only)
     */
    @PutMapping("/api/pm/comments/{commentId}")
    public BaseResponse<?> updateComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentRequest request
    ) {
        return taskCommentService.updateComment(userId, commentId, request);
    }

    /**
     * DELETE /api/pm/comments/{commentId}
     * Delete a comment (creator or project owner)
     */
    @DeleteMapping("/api/pm/comments/{commentId}")
    public BaseResponse<?> deleteComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId
    ) {
        return taskCommentService.deleteComment(userId, commentId);
    }
}
