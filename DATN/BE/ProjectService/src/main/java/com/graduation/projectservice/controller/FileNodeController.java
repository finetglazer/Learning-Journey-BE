package com.graduation.projectservice.controller;

import com.graduation.projectservice.model.PM_FileNode;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.FileNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm")
@RequiredArgsConstructor
public class FileNodeController {

    private final FileNodeService fileNodeService;

    // GET /api/pm/projects/{projectId}/files?parent_node_id=...
    @GetMapping("/projects/{projectId}/files")
    public ResponseEntity<?> getFiles(
            @PathVariable Long projectId,
            @RequestParam(value = "parent_node_id", required = false) Long parentNodeId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        BaseResponse<?> response = fileNodeService.getFiles(userId, projectId, parentNodeId);
        return ResponseEntity.ok(response);
    }

    // POST /api/pm/projects/{projectId}/files/folder
    @PostMapping("/projects/{projectId}/files/folder")
    public ResponseEntity<?> createFolder(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        String name = (String) request.get("name");
        Long parentNodeId = request.get("parent_node_id") != null ?
                Long.valueOf(request.get("parent_node_id").toString()) : null;

        BaseResponse<?> response = fileNodeService.createFolder(userId, projectId, parentNodeId, name);
        return ResponseEntity.ok(response);
    }

    // POST /api/pm/projects/{projectId}/files/upload
    @PostMapping("/projects/{projectId}/files/upload")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parent_node_id", required = false) Long parentNodeId,
            @RequestHeader("X-User-Id") Long userId
    ) throws IOException {
        BaseResponse<?> response = fileNodeService.uploadFile(userId, projectId, parentNodeId, file);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/pm/files/{nodeId}
    @DeleteMapping("/projects/{projectId}/files/{nodeId}")
    public ResponseEntity<?> deleteNode(
            @PathVariable Long projectId, // <--- Added projectId to URL
            @PathVariable Long nodeId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        // Pass projectId to service for strict validation
        BaseResponse<?> response = fileNodeService.deleteNode(userId, projectId, nodeId);
        return ResponseEntity.ok(response);
    }

    // GET /api/pm/projects/{projectId}/files/search?keyword=...
    @GetMapping("/projects/{projectId}/files/search")
    public ResponseEntity<?> searchFiles(
            @PathVariable Long projectId,
            @RequestParam("keyword") String keyword,
            @RequestHeader("X-User-Id") Long userId
    ) {
        BaseResponse<?> response = fileNodeService.searchFiles(userId, projectId, keyword);
        return ResponseEntity.ok(response);
    }
}