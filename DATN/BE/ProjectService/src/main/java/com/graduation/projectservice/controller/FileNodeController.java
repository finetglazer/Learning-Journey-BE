package com.graduation.projectservice.controller;

import com.graduation.projectservice.payload.request.CreateNotionDocRequest;
import com.graduation.projectservice.payload.request.UpdateDocumentTitleRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.FileNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pm")
@RequiredArgsConstructor
public class FileNodeController {

        private final FileNodeService fileNodeService;

        // ============================================
        // Existing Endpoints
        // ============================================

        // GET
        // /api/pm/projects/{projectId}/files?parent_node_id=...&flatten=true&types=STATIC_FILE,NOTION_DOC
        @GetMapping("/projects/{projectId}/files")
        public ResponseEntity<?> getFiles(
                        @PathVariable Long projectId,
                        @RequestParam(value = "parent_node_id", required = false) Long parentNodeId,
                        @RequestParam(value = "flatten", required = false, defaultValue = "false") Boolean flatten,
                        @RequestParam(value = "types", required = false) String types,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestHeader("X-User-Id") Long userId) {
                BaseResponse<?> response = fileNodeService.getFiles(userId, projectId, parentNodeId, flatten, types, search);
                return ResponseEntity.ok(response);
        }

        // POST /api/pm/projects/{projectId}/files/folder
        @PostMapping("/projects/{projectId}/files/folder")
        public ResponseEntity<?> createFolder(
                        @PathVariable Long projectId,
                        @RequestBody Map<String, Object> request,
                        @RequestHeader("X-User-Id") Long userId) {
                String name = (String) request.get("name");
                Long parentNodeId = request.get("parent_node_id") != null
                                ? Long.valueOf(request.get("parent_node_id").toString())
                                : null;

                BaseResponse<?> response = fileNodeService.createFolder(userId, projectId, parentNodeId, name);
                return ResponseEntity.ok(response);
        }

        // POST /api/pm/projects/{projectId}/files/upload
        @PostMapping("/projects/{projectId}/files/upload")
        public ResponseEntity<?> uploadFile(
                        @PathVariable Long projectId,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(value = "parent_node_id", required = false) Long parentNodeId,
                        @RequestHeader("X-User-Id") Long userId) throws IOException {
                BaseResponse<?> response = fileNodeService.uploadFile(userId, projectId, parentNodeId, file);
                return ResponseEntity.ok(response);
        }

        /**
         * PATCH /api/pm/projects/{projectId}/files/{nodeId}/move
         * Move a file or folder to a new parent folder
         */
        @PatchMapping("/projects/{projectId}/files/{nodeId}/move")
        public ResponseEntity<?> moveNode(
                @PathVariable Long projectId,
                @PathVariable Long nodeId,
                @RequestBody Map<String, Object> request,
                @RequestHeader("X-User-Id") Long userId) {

            // Extract the new parent ID from the request body
            // If new_parent_id is null, it moves the node to the project root
            Long newParentNodeId = request.get("new_parent_id") != null
                    ? Long.valueOf(request.get("new_parent_id").toString())
                    : null;

            log.info("User {} moving node {} to parent {} in project {}", userId, nodeId, newParentNodeId, projectId);

            BaseResponse<?> response = fileNodeService.moveNode(userId, projectId, nodeId, newParentNodeId);
            return ResponseEntity.ok(response);
        }

        // DELETE /api/pm/projects/{projectId}/files/{nodeId}
        @DeleteMapping("/projects/{projectId}/files/{nodeId}")
        public ResponseEntity<?> deleteNode(
                        @PathVariable Long projectId,
                        @PathVariable Long nodeId,
                        @RequestHeader("X-User-Id") Long userId) {
                BaseResponse<?> response = fileNodeService.deleteNode(userId, projectId, nodeId);
                return ResponseEntity.ok(response);
        }

        // GET /api/pm/projects/{projectId}/files/search?keyword=...
        @GetMapping("/projects/{projectId}/files/search")
        public ResponseEntity<?> searchFiles(
                        @PathVariable Long projectId,
                        @RequestParam("keyword") String keyword,
                        @RequestHeader("X-User-Id") Long userId) {
                BaseResponse<?> response = fileNodeService.searchFiles(userId, projectId, keyword);
                return ResponseEntity.ok(response);
        }

        // ============================================
        // New Endpoints for Notion Document
        // ============================================

        /**
         * POST /api/pm/projects/{projectId}/files/document
         * Create a new Notion-style document
         */
        @PostMapping("/projects/{projectId}/files/document")
        public ResponseEntity<?> createNotionDocument(
                        @PathVariable Long projectId,
                        @RequestBody CreateNotionDocRequest request,
                        @RequestHeader("X-User-Id") Long userId) {
                log.debug("POST /api/pm/projects/{}/files/document - User: {}", projectId, userId);

                BaseResponse<?> response = fileNodeService.createNotionDocument(
                                userId,
                                projectId,
                                request.getParentNodeId(),
                                request.getName());
                return ResponseEntity.ok(response);
        }

        /**
         * GET /api/pm/files/{nodeId}
         * Get document details (for editor page load)
         */
        @GetMapping("/files/{nodeId}")
        public ResponseEntity<?> getDocumentDetails(
                        @PathVariable Long nodeId,
                        @RequestHeader("X-User-Id") Long userId) {
                log.debug("GET /api/pm/files/{} - User: {}", nodeId, userId);

                BaseResponse<?> response = fileNodeService.getDocumentDetails(userId, nodeId);
                return ResponseEntity.ok(response);
        }

        /**
         * GET /api/pm/files/{nodeId}/history
         * Get version history for a document
         */
        @GetMapping("/files/{nodeId}/history")
        public ResponseEntity<?> getVersionHistory(
                        @PathVariable Long nodeId,
                        @RequestHeader("X-User-Id") Long userId) {
                log.debug("GET /api/pm/files/{}/history - User: {}", nodeId, userId);

                BaseResponse<?> response = fileNodeService.getVersionHistory(userId, nodeId);
                return ResponseEntity.ok(response);
        }

        /**
         * POST /api/pm/files/{nodeId}/history/{versionId}/restore
         * Restore document to a previous version
         */
        @PostMapping("/files/{nodeId}/history/{versionId}/restore")
        public ResponseEntity<?> restoreVersion(
                        @PathVariable Long nodeId,
                        @PathVariable String versionId, // ✅ FIX: Change Long to String
                        @RequestHeader("X-User-Id") Long userId) {
                log.debug("POST .../restore - User: {}, Version: {}", userId, versionId);

                BaseResponse<?> response = fileNodeService.restoreVersion(userId, nodeId, versionId);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/projects/files/{nodeId}/update-title")
        public ResponseEntity<?> updateTitleDocument(
                        @PathVariable Long nodeId,
                        @RequestBody UpdateDocumentTitleRequest request,
                        @RequestHeader("X-User-Id") Long userId) {
                log.debug("POST /api/pm/files/{}/update-title - User: {}, Name: {}", nodeId, userId, request.getName());

                BaseResponse<?> response = fileNodeService.updateDocument(userId, nodeId, request.getName());
                return ResponseEntity.ok(response);
        }

        /**
         * POST /api/pm/projects/{projectId}/editor-images/upload
         * Upload image for Notion editor (stored separately, does not create file node)
         */
        @PostMapping("/projects/{projectId}/editor-images/upload")
        public ResponseEntity<?> uploadEditorImage(
                        @PathVariable Long projectId,
                        @RequestParam("file") MultipartFile file,
                        @RequestHeader("X-User-Id") Long userId) throws IOException {
                log.debug("POST /api/pm/projects/{}/editor-images/upload - User: {}, File: {}",
                                projectId, userId, file.getOriginalFilename());

                BaseResponse<?> response = fileNodeService.uploadEditorImage(userId, projectId, file);
                return ResponseEntity.ok(response);
        }
}