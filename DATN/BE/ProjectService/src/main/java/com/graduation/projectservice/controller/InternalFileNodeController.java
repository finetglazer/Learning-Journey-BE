package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.FileNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/internal/pm")
@RequiredArgsConstructor
public class InternalFileNodeController {

    private final FileNodeService fileNodeService;

    /**
     * POST /api/internal/pm/files/{nodeId}/version/sync
     * Called by Document Service to record a new snapshot
     */
    @PostMapping("/files/{nodeId}/version/sync")
    public ResponseEntity<?> syncVersion(
            @PathVariable Long nodeId,
            @RequestBody Map<String, Object> request
    ) {
        String snapshotRef = (String) request.get("snapshotRef");
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : 0L;
        String reason = (String) request.get("reason");

        log.info("Internal Sync: Recording snapshot {} for node {}", snapshotRef, nodeId);

        fileNodeService.syncVersion(nodeId, snapshotRef, userId, reason);

        return ResponseEntity.ok(new BaseResponse<>(1, "Version synced successfully", null));
    }
}