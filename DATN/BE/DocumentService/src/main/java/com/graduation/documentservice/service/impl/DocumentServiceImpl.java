package com.graduation.documentservice.service.impl;

import com.graduation.documentservice.client.ProjectServiceClient;
import com.graduation.documentservice.constant.DocumentConstant;
import com.graduation.documentservice.model.CommentThread;
import com.graduation.documentservice.model.DocContent;
import com.graduation.documentservice.model.DocSnapshot;
import com.graduation.documentservice.payload.response.BaseResponse;
import com.graduation.documentservice.payload.response.DocumentDTO;
import com.graduation.documentservice.payload.response.SnapshotDTO;
import com.graduation.documentservice.repository.DocContentRepository;
import com.graduation.documentservice.repository.DocSnapshotRepository;
import com.graduation.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocContentRepository docContentRepository;
    private final DocSnapshotRepository docSnapshotRepository;
    private final ProjectServiceClient projectServiceClient;

    @Value("${app.snapshot.max-per-document:50}")
    private int maxSnapshotsPerDocument;

    @Override
    @Transactional
    public BaseResponse<?> createDocument(Long pgNodeId, Long projectId) {
        log.info(DocumentConstant.LOG_CREATING_DOCUMENT, pgNodeId, projectId);

        // Check if document already exists
        if (docContentRepository.existsByPgNodeId(pgNodeId)) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_ALREADY_EXISTS,
                    null
            );
        }

        DocContent docContent = DocContent.builder()
                .pgNodeId(pgNodeId)
                .projectId(projectId)
                .content(DocumentConstant.getEmptyDocContent())
                .threads(new ArrayList<>())
                .version(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DocContent saved = docContentRepository.save(docContent);
        log.info(DocumentConstant.LOG_DOCUMENT_CREATED, saved.getIdAsString(), pgNodeId);

        Map<String, Object> response = new HashMap<>();
        response.put("storageReference", saved.getIdAsString());
        response.put("pgNodeId", pgNodeId);

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.DOCUMENT_CREATED_SUCCESS,
                response
        );
    }

    @Override
    public BaseResponse<?> loadDocument(String storageRef) {
        log.info(DocumentConstant.LOG_LOADING_DOCUMENT, storageRef);

        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        DocContent doc = docOpt.get();
        DocumentDTO dto = DocumentDTO.builder()
                .storageReference(doc.getIdAsString())
                .pgNodeId(doc.getPgNodeId())
                .projectId(doc.getProjectId())
                .content(doc.getContent())
                .threads(doc.getThreads())
                .version(doc.getVersion())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.DOCUMENT_LOADED_SUCCESS,
                dto
        );
    }

    @Override
    @Transactional
    public BaseResponse<?> saveDocument(String storageRef, Map<String, Object> content, List<CommentThread> threads) {
        log.info(DocumentConstant.LOG_SAVING_DOCUMENT, storageRef);

        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        DocContent doc = docOpt.get();

        // Process orphaned comments
        List<CommentThread> processedThreads = processOrphanedComments(content, threads);

        doc.setContent(content);
        doc.setThreads(processedThreads);
        doc.setVersion(doc.getVersion() + 1);
        doc.setUpdatedAt(LocalDateTime.now());

        docContentRepository.save(doc);

        Map<String, Object> response = new HashMap<>();
        response.put("storageReference", storageRef);
        response.put("version", doc.getVersion());

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.DOCUMENT_SAVED_SUCCESS,
                response
        );
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteDocument(String storageRef) {
        log.info(DocumentConstant.LOG_DELETING_DOCUMENT, storageRef);

        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        DocContent doc = docOpt.get();

        // Delete all snapshots first
        docSnapshotRepository.deleteByPageId(doc.getId());

        // Delete the document
        docContentRepository.delete(doc);

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.DOCUMENT_DELETED_SUCCESS,
                null
        );
    }

    @Override
    @Transactional
    // ✅ Updated signature
    public BaseResponse<?> createSnapshot(String storageRef, String reason, Long createdBy, String createdByName, String createdByAvatar) {
        log.info(DocumentConstant.LOG_CREATING_SNAPSHOT, storageRef);

        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(DocumentConstant.ERROR_STATUS, DocumentConstant.ERROR_DOCUMENT_NOT_FOUND, null);
        }

        DocContent doc = docOpt.get();

        // ✅ Use passed info, or fallback to defaults if missing
        String finalName = (createdByName != null && !createdByName.isEmpty()) ? createdByName : "User " + createdBy;
        String finalAvatar = (createdByAvatar != null) ? createdByAvatar : "";

        DocSnapshot snapshot = DocSnapshot.builder()
                .pageId(doc.getIdAsString()) // Ensure we save String ID
                .pgNodeId(doc.getPgNodeId())
                .contentSnapshot(doc.getContent())
                .threadsSnapshot(doc.getThreads())
                .versionAtSnapshot(doc.getVersion())
                .reason(reason)
                .createdBy(createdBy)
                .createdByName(finalName)   // ✅ Save real name
                .createdByAvatar(finalAvatar) // ✅ Save real avatar
                .createdAt(LocalDateTime.now())
                .build();

        DocSnapshot saved = docSnapshotRepository.save(snapshot);
        enforceMaxSnapshots(doc.getPgNodeId());


        // ✅ NEW: Sync to Project Service immediately
        // We run this asynchronously or safely so if it fails, it doesn't rollback the Mongo save
        try {
            projectServiceClient.syncSnapshotToVersion(
                    doc.getPgNodeId(),      // 30
                    saved.getIdAsString(),  // "693d..."
                    createdBy,              // 35
                    reason                  // "SESSION_END"
            );
        } catch (Exception e) {
            log.error("Failed to sync version to Project Service", e);
            // Don't throw exception here, or you lose the MongoDB data
        }
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("snapshotId", saved.getIdAsString());
        responseData.put("versionNumber", saved.getVersionAtSnapshot());

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.SNAPSHOT_CREATED_SUCCESS,
                responseData // Sending a Map instead of String
        );
    }

    @Override
    public BaseResponse<?> getSnapshot(String storageRef, String snapshotId) {
        log.info(DocumentConstant.LOG_LOADING_SNAPSHOT, snapshotId, storageRef);

        // Validate document exists
        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        // Find snapshot
        if (!ObjectId.isValid(snapshotId)) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_SNAPSHOT_NOT_FOUND,
                    null
            );
        }

        Optional<DocSnapshot> snapshotOpt = docSnapshotRepository.findById(new ObjectId(snapshotId));
        if (snapshotOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_SNAPSHOT_NOT_FOUND,
                    null
            );
        }

        DocSnapshot snapshot = snapshotOpt.get();

        // Verify snapshot belongs to this document
        if (!snapshot.getPageId().equals(docOpt.get().getId())) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_SNAPSHOT_NOT_FOUND,
                    null
            );
        }

        SnapshotDTO dto = SnapshotDTO.builder()
                .snapshotId(snapshot.getIdAsString())
                .pgNodeId(snapshot.getPgNodeId())
                .contentSnapshot(snapshot.getContentSnapshot())
                .threadsSnapshot(snapshot.getThreadsSnapshot())
                .versionAtSnapshot(snapshot.getVersionAtSnapshot())
                .createdAt(snapshot.getCreatedAt())
                .build();

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.SNAPSHOT_LOADED_SUCCESS,
                dto
        );
    }

    // In DocumentServiceImpl.java

    @Override
    @Transactional
    public BaseResponse<?> restoreToSnapshot(String storageRef, String snapshotId) {
        // 1. Find the Snapshot
        DocSnapshot snapshot = docSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));

        // 2. Find the Live Document
        DocContent doc = docContentRepository.findById(storageRef)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // 3. 🛑 THE CRITICAL STEP: Overwrite live content with snapshot content
        doc.setContent(snapshot.getContentSnapshot()); // Assuming field is 'contentSnapshot'
        doc.setUpdatedAt(LocalDateTime.now());

        docContentRepository.save(doc);

        log.info("Restored document {} to snapshot {}", storageRef, snapshotId);
        return new BaseResponse<>(DocumentConstant.SUCCESS_STATUS, "Restored successfully", null);
    }

    @Override
    public BaseResponse<?> getSnapshotList(String storageRef) {
        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(DocumentConstant.ERROR_STATUS, DocumentConstant.ERROR_DOCUMENT_NOT_FOUND, null);
        }

        DocContent doc = docOpt.get();

        // 🛑 CRITICAL FIX: Use getIdAsString() instead of getId()
        // Your DB stores pageId as "693c..." (String), but doc.getId() is an ObjectId object.
        // This mismatch caused the empty list.
        List<DocSnapshot> snapshots = docSnapshotRepository.findByPageIdOrderByCreatedAtDesc(doc.getIdAsString());

        List<Map<String, Object>> snapshotList = snapshots.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    // ✅ FIX 1: Map the MongoDB ID to 'snapshotRef' (Matches Frontend DTO)
                    map.put("snapshotRef", s.getIdAsString());

                    // ✅ FIX 2: Provide a dummy 'versionId' (Matches Frontend DTO type: number)
                    // The Document Service doesn't know the SQL ID, but that's okay.
                    // The Frontend uses 'snapshotRef' for restoring now.
                    map.put("versionId", 0);
                    map.put("versionNumber", s.getVersionAtSnapshot());
                    map.put("createdAt", s.getCreatedAt());
                    map.put("reason", s.getReason());
                    map.put("createdByName", s.getCreatedByName());
                    map.put("createdByAvatar", s.getCreatedByAvatar());
                    // ✅ Add this if you want to verify the user ID in frontend
                    map.put("createdBy", s.getCreatedBy());
                    return map;
                })
                .collect(Collectors.toList());

        return new BaseResponse<>(DocumentConstant.SUCCESS_STATUS, "Snapshots retrieved", snapshotList);
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private Optional<DocContent> findByStorageRef(String storageRef) {
        if (storageRef == null || !ObjectId.isValid(storageRef)) {
            return Optional.empty();
        }
        return docContentRepository.findById(new ObjectId(storageRef));
    }

    /**
     * Process orphaned comments - comments whose marks no longer exist in content
     */
    private List<CommentThread> processOrphanedComments(Map<String, Object> content, List<CommentThread> threads) {
        if (threads == null || threads.isEmpty()) {
            return threads;
        }

        // Extract all comment IDs from content
        Set<String> activeCommentIds = extractCommentIdsFromContent(content);

        // Mark orphaned comments as resolved
        for (CommentThread thread : threads) {
            // ✅ FIX 2: Use getThreadId() instead of getId()
            // And ensure we check for null ID to avoid crashes
            if (thread.getThreadId() != null && !Boolean.TRUE.equals(thread.getResolved()) && !activeCommentIds.contains(thread.getThreadId())) {
                thread.setResolved(true);
                thread.setResolvedReason(DocumentConstant.RESOLVED_ORPHANED);
                thread.setResolvedAt(LocalDateTime.now());
            }
        }

        return threads;
    }

    /**
     * Recursively extract comment IDs from Tiptap content structure
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractCommentIdsFromContent(Map<String, Object> content) {
        Set<String> commentIds = new HashSet<>();
        if (content == null) {
            return commentIds;
        }

        // Check for marks in this node
        Object marks = content.get("marks");
        if (marks instanceof List) {
            for (Object mark : (List<?>) marks) {
                if (mark instanceof Map) {
                    Map<String, Object> markMap = (Map<String, Object>) mark;
                    if ("comment".equals(markMap.get("type"))) {
                        Object attrs = markMap.get("attrs");
                        if (attrs instanceof Map) {
                            // ✅ FIX 3: Look for "threadId", NOT "commentId"
                            // Your frontend extension saves it as "threadId"
                            Object threadIdVal = ((Map<String, Object>) attrs).get("threadId");
                            if (threadIdVal != null) {
                                commentIds.add(threadIdVal.toString());
                            }
                        }
                    }
                }
            }
        }

        // Recursively check content array... (rest is fine)
        Object contentArray = content.get("content");
        if (contentArray instanceof List) {
            for (Object child : (List<?>) contentArray) {
                if (child instanceof Map) {
                    commentIds.addAll(extractCommentIdsFromContent((Map<String, Object>) child));
                }
            }
        }

        return commentIds;
    }
    /**
     * Enforce maximum snapshots per document
     */
    private void enforceMaxSnapshots(Long pgNodeId) {
        long count = docSnapshotRepository.countByPgNodeId(pgNodeId);

        if (count > maxSnapshotsPerDocument) {
            log.info(DocumentConstant.LOG_MAX_SNAPSHOTS_CLEANUP, pgNodeId, count, maxSnapshotsPerDocument);

            // Get oldest snapshots (sorted by createdAt ascending)
            List<DocSnapshot> allSnapshots = docSnapshotRepository.findByPgNodeIdOrderByCreatedAtAsc(pgNodeId);

            // Delete excess oldest snapshots
            int toDelete = (int) (count - maxSnapshotsPerDocument);
            for (int i = 0; i < toDelete && i < allSnapshots.size(); i++) {
                docSnapshotRepository.delete(allSnapshots.get(i));
            }
        }
    }
}
