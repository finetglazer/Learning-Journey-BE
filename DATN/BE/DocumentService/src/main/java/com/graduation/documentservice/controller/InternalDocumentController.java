package com.graduation.documentservice.controller;

import com.graduation.documentservice.payload.request.CreateDocumentRequest;
import com.graduation.documentservice.payload.request.CreateSnapshotRequest;
import com.graduation.documentservice.payload.request.SaveDocumentRequest;
import com.graduation.documentservice.payload.response.BaseResponse;
import com.graduation.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/documents")
@RequiredArgsConstructor
public class InternalDocumentController {

    private final DocumentService documentService;

    /**
     * POST /api/internal/documents
     * Create a new empty document
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createDocument(@RequestBody CreateDocumentRequest request) {
        log.debug("POST /api/internal/documents - pgNodeId: {}, projectId: {}",
                request.getPgNodeId(), request.getProjectId());

        BaseResponse<?> response = documentService.createDocument(
                request.getPgNodeId(),
                request.getProjectId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/internal/documents/{storageRef}
     * Load document content
     */
    @GetMapping("/{storageRef}")
    public ResponseEntity<BaseResponse<?>> loadDocument(@PathVariable String storageRef) {
        log.debug("GET /api/internal/documents/{}", storageRef);

        BaseResponse<?> response = documentService.loadDocument(storageRef);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/internal/documents/{storageRef}
     * Save/update document (called by Hocuspocus)
     */
    @PutMapping("/{storageRef}")
    public ResponseEntity<BaseResponse<?>> saveDocument(
            @PathVariable String storageRef,
            @RequestBody SaveDocumentRequest request
    ) {
        log.debug("PUT /api/internal/documents/{}", storageRef);

        BaseResponse<?> response = documentService.saveDocument(
                storageRef,
                request.getContent(),
                request.getThreads()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/internal/documents/{storageRef}
     * Delete document and all snapshots
     */
    @DeleteMapping("/{storageRef}")
    public ResponseEntity<BaseResponse<?>> deleteDocument(@PathVariable String storageRef) {
        log.debug("DELETE /api/internal/documents/{}", storageRef);

        BaseResponse<?> response = documentService.deleteDocument(storageRef);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{storageRef}/snapshot")
    public ResponseEntity<BaseResponse<?>> createSnapshot(
            @PathVariable String storageRef,
            @RequestBody CreateSnapshotRequest request
    ) {
        log.debug("POST .../snapshot - reason: {}, user: {}", request.getReason(), request.getCreatedByName());

        BaseResponse<?> response = documentService.createSnapshot(
                storageRef,
                request.getReason(),
                request.getCreatedBy(),
                request.getCreatedByName(),   // ✅ Pass Name
                request.getCreatedByAvatar()  // ✅ Pass Avatar
        );
        return ResponseEntity.ok(response);
    }


    /**
     * GET /api/internal/documents/{storageRef}/snapshot/{snapshotId}
     * Get specific snapshot content
     */
    @GetMapping("/{storageRef}/snapshot/{snapshotId}")
    public ResponseEntity<BaseResponse<?>> getSnapshot(
            @PathVariable String storageRef,
            @PathVariable String snapshotId
    ) {
        log.debug("GET /api/internal/documents/{}/snapshot/{}", storageRef, snapshotId);

        BaseResponse<?> response = documentService.getSnapshot(storageRef, snapshotId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/internal/documents/{storageRef}/snapshots
     * Get list of all snapshots for a document
     */
    @GetMapping("/{storageRef}/snapshots")
    public ResponseEntity<BaseResponse<?>> getSnapshotList(@PathVariable String storageRef) {
        log.debug("GET /api/internal/documents/{}/snapshots", storageRef);

        BaseResponse<?> response = documentService.getSnapshotList(storageRef);
        return ResponseEntity.ok(response);
    }

    // POST /api/internal/documents/{storageRef}/restore/{snapshotId}
    @PostMapping("/{storageRef}/restore/{snapshotId}")
    public ResponseEntity<BaseResponse<?>> restoreDocument(
            @PathVariable String storageRef,
            @PathVariable String snapshotId
    ) {

        BaseResponse<?> response = documentService.restoreToSnapshot(storageRef, snapshotId);
        return ResponseEntity.ok(response);
    }
}
