package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.response.BaseResponse;
import com.graduation.forumservice.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/forum")
@RequiredArgsConstructor
public class ForumInternalController {

    private final PostService postService;

    /**
     * GET /api/internal/forum/projects/{projectId}
     * Retrieves all posts shared/saved to a specific project.
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<BaseResponse<?>> getSharedPostsByProject(
            @PathVariable Long projectId) {

        log.info("Internal request: Fetching shared posts for projectId {}", projectId);

        BaseResponse<?> response = postService.getSharedPostsByProject(projectId);

        return ResponseEntity.ok(response);
    }
}