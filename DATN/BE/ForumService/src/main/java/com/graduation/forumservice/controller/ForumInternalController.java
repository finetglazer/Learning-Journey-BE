package com.graduation.forumservice.controller;

import com.graduation.forumservice.payload.response.BaseResponse;
import com.graduation.forumservice.service.ForumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/forum")
@RequiredArgsConstructor
public class ForumInternalController {

    private final ForumService forumService;

    /**
     * GET /api/internal/forum/projects/{projectId}
     * Retrieves all posts shared/saved to a specific project.
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<BaseResponse<?>> getSharedPostsByProject(
            @PathVariable Long projectId) {

        log.info("Internal request: Fetching shared posts for projectId {}", projectId);

        // Optional: Simple manual check if not using a global security filter
        // if (!isValidInternalKey(internalKey)) return unauthorizedResponse();

        BaseResponse<?> response = forumService.getSharedPostsByProject(projectId);

        return ResponseEntity.ok(response);
    }
}