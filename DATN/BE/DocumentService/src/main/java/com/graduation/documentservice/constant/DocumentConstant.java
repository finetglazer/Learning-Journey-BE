package com.graduation.documentservice.constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentConstant {

    // ============================================
    // Response Status Codes
    // ============================================
    public static final int SUCCESS_STATUS = 1;
    public static final int ERROR_STATUS = 0;

    // ============================================
    // Success Messages
    // ============================================
    public static final String DOCUMENT_CREATED_SUCCESS = "Document created successfully";
    public static final String DOCUMENT_LOADED_SUCCESS = "Document loaded successfully";
    public static final String DOCUMENT_SAVED_SUCCESS = "Document saved successfully";
    public static final String DOCUMENT_DELETED_SUCCESS = "Document deleted successfully";
    public static final String SNAPSHOT_CREATED_SUCCESS = "Snapshot created successfully";
    public static final String SNAPSHOT_LOADED_SUCCESS = "Snapshot loaded successfully";

    // ============================================
    // Error Messages
    // ============================================
    public static final String ERROR_DOCUMENT_NOT_FOUND = "Document not found";
    public static final String ERROR_SNAPSHOT_NOT_FOUND = "Snapshot not found";
    public static final String ERROR_INVALID_STORAGE_REF = "Invalid storage reference";
    public static final String ERROR_DOCUMENT_ALREADY_EXISTS = "Document already exists for this node";

    // ============================================
    // Log Messages
    // ============================================
    public static final String LOG_CREATING_DOCUMENT = "Creating document for pgNodeId: {}, projectId: {}";
    public static final String LOG_DOCUMENT_CREATED = "Document created with id: {} for pgNodeId: {}";
    public static final String LOG_LOADING_DOCUMENT = "Loading document with storageRef: {}";
    public static final String LOG_SAVING_DOCUMENT = "Saving document with storageRef: {}";
    public static final String LOG_DELETING_DOCUMENT = "Deleting document with storageRef: {}";
    public static final String LOG_CREATING_SNAPSHOT = "Creating snapshot for storageRef: {}";
    public static final String LOG_LOADING_SNAPSHOT = "Loading snapshot: {} for storageRef: {}";
    public static final String LOG_CLEANUP_STARTED = "Starting snapshot cleanup job";
    public static final String LOG_CLEANUP_COMPLETED = "Snapshot cleanup completed. Deleted {} old snapshots";
    public static final String LOG_MAX_SNAPSHOTS_CLEANUP = "Enforcing max snapshots for pgNodeId: {}. Current: {}, Max: {}";

    // ============================================
    // Snapshot Reasons
    // ============================================
    public static final String REASON_AUTO_30MIN = "AUTO_30MIN";
    public static final String REASON_SESSION_END = "SESSION_END";
    public static final String REASON_RESTORED = "RESTORED";
    public static final String REASON_MANUAL = "MANUAL";

    // ============================================
    // Comment Resolved Reasons
    // ============================================
    public static final String RESOLVED_MANUAL = "MANUAL";
    public static final String RESOLVED_ORPHANED = "ORPHANED";

    // ============================================
    // Default Content
    // ============================================
    public static Map<String, Object> getEmptyDocContent() {
        Map<String, Object> emptyDoc = new HashMap<>();
        emptyDoc.put("type", "doc");
        emptyDoc.put("content", List.of());
        return emptyDoc;
    }
}
