package com.graduation.documentservice.controller;

import com.graduation.documentservice.payload.response.BaseResponse;
import com.graduation.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class PublicDocumentController {

    private final DocumentService documentService;

    /**
     * GET /api/document/{storageRef}/snapshots
     * Get list of all snapshots for a document (History)
     */
    @GetMapping("/{storageRef}/snapshots")
    public ResponseEntity<BaseResponse<?>> getHistory(
            @PathVariable String storageRef,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        // We log the User ID to trace who is accessing the history
        log.debug("GET /api/document/{}/snapshots - User: {}", storageRef, userId);

        return ResponseEntity.ok(documentService.getSnapshotList(storageRef));
    }
}