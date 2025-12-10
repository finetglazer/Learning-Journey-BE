package com.graduation.documentservice.service;

import com.graduation.documentservice.model.CommentThread;
import com.graduation.documentservice.payload.response.BaseResponse;

import java.util.List;
import java.util.Map;

public interface DocumentService {

    /**
     * Create a new empty document
     */
    BaseResponse<?> createDocument(Long pgNodeId, Long projectId);

    /**
     * Load document by storage reference (MongoDB ObjectId hex string)
     */
    BaseResponse<?> loadDocument(String storageRef);

    /**
     * Save/update document content and threads
     */
    BaseResponse<?> saveDocument(String storageRef, Map<String, Object> content, List<CommentThread> threads);

    /**
     * Delete document and all its snapshots
     */
    BaseResponse<?> deleteDocument(String storageRef);

    /**
     * Create a version snapshot
     */
    BaseResponse<?> createSnapshot(String storageRef, String reason, Long createdBy);

    /**
     * Get snapshot content by snapshot ID
     */
    BaseResponse<?> getSnapshot(String storageRef, String snapshotId);

    /**
     * Get list of all snapshots for a document (metadata only)
     */
    BaseResponse<?> getSnapshotList(String storageRef);
}
