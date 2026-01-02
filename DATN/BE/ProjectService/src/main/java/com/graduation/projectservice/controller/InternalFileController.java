package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.FileNodeService;
import com.graduation.projectservice.service.ProjectFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pm/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final FileNodeService fileNodeService;
    private final ProjectFileStorageService storageService;

    /**
     * GET /api/internal/files/{storageRef}/access?user_id=...
     * Validate document access for WebSocket authentication
     * Called by Hocuspocus server via API Gateway
     */
    @GetMapping("/{storageRef}/access")
    public ResponseEntity<?> validateDocumentAccess(
            @PathVariable String storageRef,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.debug("GET /api/internal/files/{}/access - User: {}", storageRef, userId);
        
        BaseResponse<?> response = fileNodeService.validateDocumentAccess(userId, storageRef);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{projectId}/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<?>> uploadMultipleFiles(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestPart("files") List<MultipartFile> files) {

        log.info("POST /upload-multiple - User {} uploading {} files to project {}",
                userId, files.size(), projectId);

        try {
            // Use the multiple upload service logic
            List<String> fileUrls = storageService.uploadMultipleFiles(files);

            return ResponseEntity.ok(new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    "Files uploaded successfully",
                    Map.of("urls", fileUrls)
            ));
        } catch (IOException e) {
            log.error("Failed to upload files for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(Constant.ERROR_STATUS, "File upload failed: " + e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null));
        }
    }

    @PostMapping("/delete-multiple")
    public ResponseEntity<BaseResponse<?>> deleteMultipleFiles(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<String> fileUrls) {

        log.info("DELETE /delete-multiple - User {} requested deletion of {} files", userId, fileUrls.size());

        try {
            // Calls the storage service logic to remove objects from GCS
            storageService.deleteMultipleFiles(fileUrls);

            return ResponseEntity.ok(new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    "Files deleted successfully from storage",
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to delete multiple files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(Constant.ERROR_STATUS, "File deletion failed: " + e.getMessage(), null));
        }
    }

    /**
     * POST /api/pm/internal/files/{projectId}/save-to-project
     * Uploads a file and creates a FileNode in the project directory.
     */
    @PostMapping(value = "/{projectId}/save-to-project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<?>> saveFileToProject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @RequestParam(value = "parentNodeId", required = false) Long parentNodeId,
            @RequestPart("file") MultipartFile file) {

        log.info("POST /save-to-project - User {} saving file '{}' to project {}",
                userId, file.getOriginalFilename(), projectId);

        try {
            // fileNodeService.uploadFile handles both GCS storage and DB node creation
            BaseResponse<?> response = fileNodeService.uploadFile(userId, projectId, parentNodeId, file);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("IO Error saving file to project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(Constant.ERROR_STATUS, "File storage failed", null));
        } catch (Exception e) {
            log.error("Unexpected error saving file to project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null));
        }
    }
}
