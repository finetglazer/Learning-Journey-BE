package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.SaveFileToProjectRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileNodeService {

    // ============================================
    // Existing Methods
    // ============================================

    BaseResponse<?> getFiles(Long userId, Long projectId, Long parentNodeId, Boolean flatten, String types, String search);

    BaseResponse<?> createFolder(Long userId, Long projectId, Long parentNodeId, String name);

    BaseResponse<?> uploadFile(Long userId, Long projectId, Long parentNodeId, MultipartFile file) throws IOException;

    BaseResponse<?> deleteNode(Long userId, Long projectId, Long nodeId);

    BaseResponse<?> searchFiles(Long userId, Long projectId, String keyword);

    // ============================================
    // New Methods for Notion Document
    // ============================================

    /**
     * Create a new Notion-style document
     * Creates entry in Postgres and empty document shell in MongoDB
     */
    BaseResponse<?> createNotionDocument(Long userId, Long projectId, Long parentNodeId, String name);

    /**
     * Get document details including storage reference for WebSocket connection
     */
    BaseResponse<?> getDocumentDetails(Long userId, Long nodeId);

    /**
     * Get version history for a document
     */
    BaseResponse<?> getVersionHistory(Long userId, Long nodeId);

    // Update signature
    BaseResponse<?> restoreVersion(Long userId, Long nodeId, String versionId);

    /**
     * Internal: Validate document access for WebSocket authentication
     * Called by Hocuspocus server via API Gateway
     */
    BaseResponse<?> validateDocumentAccess(Long userId, String storageRef);

    BaseResponse<?> updateDocument(Long userId, Long nodeId, String name);


    BaseResponse<?> syncVersion(Long nodeId, String snapshotRef, Long userId, String reason);

    /**
     * Upload image for editor (stores in separate GCS path, does not create file
     * node)
     */
    BaseResponse<?> uploadEditorImage(Long userId, Long projectId, MultipartFile file) throws IOException;

    BaseResponse<?> saveFileToProject(SaveFileToProjectRequest request);

    /**
     * Move a file or folder to a new parent directory within the same project.
     * Handles updating parentNodeId and validating hierarchical integrity.
     */
    BaseResponse<?> moveNode(Long userId, Long projectId, Long nodeId, Long newParentId);
}