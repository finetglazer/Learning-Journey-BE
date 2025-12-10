package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.FileNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final FileNodeService fileNodeService;

    /**
     * GET /api/internal/files/{storageRef}/access?user_id=...
     * Validate document access for WebSocket authentication
     * Called by Hocuspocus server via API Gateway
     */
    @GetMapping("/{storageRef}/access")
    public ResponseEntity<?> validateDocumentAccess(
            @PathVariable String storageRef,
            @RequestParam("user_id") Long userId
    ) {
        log.debug("GET /api/internal/files/{}/access - User: {}", storageRef, userId);
        
        BaseResponse<?> response = fileNodeService.validateDocumentAccess(userId, storageRef);
        return ResponseEntity.ok(response);
    }
}
