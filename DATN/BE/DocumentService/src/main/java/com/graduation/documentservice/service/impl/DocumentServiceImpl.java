package com.graduation.documentservice.service.impl;

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
    public BaseResponse<?> createSnapshot(String storageRef, String reason, Long createdBy) {
        log.info(DocumentConstant.LOG_CREATING_SNAPSHOT, storageRef);

        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        DocContent doc = docOpt.get();

        // Create snapshot
        DocSnapshot snapshot = DocSnapshot.builder()
                .pageId(doc.getId())
                .pgNodeId(doc.getPgNodeId())
                .contentSnapshot(doc.getContent())
                .threadsSnapshot(doc.getThreads())
                .versionAtSnapshot(doc.getVersion())
                .createdAt(LocalDateTime.now())
                .build();

        DocSnapshot saved = docSnapshotRepository.save(snapshot);

        // Enforce max snapshots limit
        enforceMaxSnapshots(doc.getPgNodeId());

        Map<String, Object> response = new HashMap<>();
        response.put("snapshotId", saved.getIdAsString());
        response.put("pgNodeId", doc.getPgNodeId());
        response.put("versionAtSnapshot", doc.getVersion());
        response.put("reason", reason);
        response.put("createdBy", createdBy);

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                DocumentConstant.SNAPSHOT_CREATED_SUCCESS,
                response
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

    @Override
    public BaseResponse<?> getSnapshotList(String storageRef) {
        Optional<DocContent> docOpt = findByStorageRef(storageRef);
        if (docOpt.isEmpty()) {
            return new BaseResponse<>(
                    DocumentConstant.ERROR_STATUS,
                    DocumentConstant.ERROR_DOCUMENT_NOT_FOUND,
                    null
            );
        }

        DocContent doc = docOpt.get();
        List<DocSnapshot> snapshots = docSnapshotRepository.findByPageIdOrderByCreatedAtDesc(doc.getId());

        List<Map<String, Object>> snapshotList = snapshots.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("snapshotId", s.getIdAsString());
                    map.put("versionAtSnapshot", s.getVersionAtSnapshot());
                    map.put("createdAt", s.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        return new BaseResponse<>(
                DocumentConstant.SUCCESS_STATUS,
                "Snapshots retrieved",
                snapshotList
        );
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
            if (!thread.getResolved() && !activeCommentIds.contains(thread.getId())) {
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
                            Object commentId = ((Map<String, Object>) attrs).get("commentId");
                            if (commentId != null) {
                                commentIds.add(commentId.toString());
                            }
                        }
                    }
                }
            }
        }

        // Recursively check content array
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
