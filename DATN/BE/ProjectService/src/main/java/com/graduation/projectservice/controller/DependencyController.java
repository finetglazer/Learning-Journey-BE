package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.request.DependencyRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.DependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}/dependencies")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    // GET /api/pm/projects/{projectId}/dependencies?itemId=1001&itemType=TASK
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getDependencies(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestParam Long itemId,
            @RequestParam String itemType) {

        return ResponseEntity.ok(dependencyService.getDependencies(userId, projectId, itemId, itemType));
    }

    // POST /api/pm/projects/{projectId}/dependencies
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createDependency(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody DependencyRequest request) {

        return ResponseEntity.ok(dependencyService.createDependency(userId, projectId, request));
    }

    // DELETE /api/pm/projects/{projectId}/dependencies
    @DeleteMapping
    public ResponseEntity<BaseResponse<?>> deleteDependency(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestBody DependencyRequest request) {

        return ResponseEntity.ok(dependencyService.deleteDependency(userId, projectId, request));
    }
}