package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.response.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileNodeService {

    // ============================================
    // Existing Methods
    // ============================================

    BaseResponse<?> getFiles(Long userId, Long projectId, Long parentNodeId);

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

    /**
     * Restore a document to a previous version
     */
    BaseResponse<?> restoreVersion(Long userId, Long nodeId, Long versionId);

    /**
     * Internal: Validate document access for WebSocket authentication
     * Called by Hocuspocus server via API Gateway
     */
    BaseResponse<?> validateDocumentAccess(Long userId, String storageRef);
}