package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.DocumentServiceClient;
import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.exception.ForbiddenException;
import com.graduation.projectservice.exception.NotFoundException;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_DocVersion;
import com.graduation.projectservice.model.PM_FileNode;
import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMembershipRole;
import com.graduation.projectservice.model.enums.NodeType;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.DocVersionRepository;
import com.graduation.projectservice.repository.FileNodeRepository;
import com.graduation.projectservice.service.FileNodeService;
import com.graduation.projectservice.service.ProjectFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileNodeServiceImpl implements FileNodeService {

    private final FileNodeRepository fileNodeRepository;
    private final DocVersionRepository docVersionRepository;
    private final ProjectFileStorageService storageService;
    private final DocumentServiceClient documentServiceClient;
    private final UserServiceClient userServiceClient;
    private final ProjectAuthorizationHelper authHelper;

    // ============================================
    // Existing Methods (unchanged)
    // ============================================

    @Override
    public BaseResponse<?> getFiles(Long userId, Long projectId, Long parentNodeId, Boolean flatten, String types, String search) {
        authHelper.requireActiveMember(projectId, userId);

        if (search == null) {
            search = "";
        }

        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null)
                return new BaseResponse<>(0, error, null);
        }

        // 1. Parse type filters if provided
        List<NodeType> typeFilters = new ArrayList<>();
        if (types != null && !types.trim().isEmpty()) {
            String[] typeArray = types.split(",");
            for (String type : typeArray) {
                try {
                    typeFilters.add(NodeType.valueOf(type.trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid NodeType filter: {}", type);
                }
            }
        }

        // 2. Fetch File Nodes
        List<PM_FileNode> files;
        if (flatten != null && flatten) {
            // Flatten: Get all files recursively from entire project
            files = fileNodeRepository.findByProjectId(projectId);
            // Filter out folders since we only want files for the picker
            files = files.stream()
                    .filter(f -> f.getType() != NodeType.FOLDER)
                    .collect(Collectors.toList());
        } else {
            // Original behavior: Get files by parent
            if (parentNodeId == null) {
                files = fileNodeRepository.findByProjectIdAndParentNodeIdIsNullAndNameContainingIgnoreCase(projectId, search);
            } else {
                files = fileNodeRepository.findByProjectIdAndParentNodeIdAndNameContainingIgnoreCase(projectId, parentNodeId, search);
            }
        }

        // 3. Apply type filtering if provided
        if (!typeFilters.isEmpty()) {
            files = files.stream()
                    .filter(f -> typeFilters.contains(f.getType()))
                    .collect(Collectors.toList());
        }

        // 4. Collect distinct User IDs to minimize API calls
        List<Long> userIds = files.stream()
                .map(PM_FileNode::getCreatedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 5. Batch fetch user info from User Service
        Map<Long, UserBatchDTO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                List<UserBatchDTO> users = userServiceClient.findUsersByIds(userIds);
                userMap = users.stream()
                        .collect(Collectors.toMap(UserBatchDTO::getUserId, Function.identity()));
            } catch (Exception e) {
                log.error("Error fetching user details for file list: {}", e.getMessage());
                // Proceed without crashing; names will default to "Unknown"
            }
        }

        // 6. Map entities to DTOs with User details
        Map<Long, UserBatchDTO> finalUserMap = userMap;

        List<FileNodeResponseDTO> responseList = files.stream().map(node -> {
            UserBatchDTO user = finalUserMap.get(node.getCreatedByUserId());
            String createdByName = (user != null) ? user.getName() : "Unknown";
            String avatarUrl = (user != null) ? user.getAvatarUrl() : null;

            return FileNodeResponseDTO.builder()
                    .nodeId(node.getNodeId())
                    .parentNodeId(node.getParentNodeId())
                    .name(node.getName())
                    .type(node.getType())
                    .updatedAt(node.getUpdatedAt())
                    .createdBy(createdByName)
                    .avatarUrl(avatarUrl)
                    .sizeBytes(node.getSizeBytes())
                    .extension(node.getExtension())
                    .storageReference(node.getStorageReference())
                    .build();
        }).collect(Collectors.toList());

        return new BaseResponse<>(1, "File list retrieved", responseList);
    }

    @Override
    @Transactional
    public BaseResponse<?> createFolder(Long userId, Long projectId, Long parentNodeId, String name) {
        authHelper.requireActiveMember(projectId, userId);

        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null)
                return new BaseResponse<>(0, error, null);
        }

        PM_FileNode folder = new PM_FileNode();
        folder.setProjectId(projectId);
        folder.setParentNodeId(parentNodeId);
        folder.setName(name);
        folder.setType(NodeType.FOLDER);
        folder.setCreatedByUserId(userId);

        PM_FileNode savedFolder = fileNodeRepository.save(folder);
        return new BaseResponse<>(1, "Folder created successfully", savedFolder);
    }

    @Override
    @Transactional
    public BaseResponse<?> uploadFile(Long userId, Long projectId, Long parentNodeId, MultipartFile file)
            throws IOException {
        authHelper.requireActiveMember(projectId, userId);

        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null)
                return new BaseResponse<>(0, error, null);
        }

        String storageRef = storageService.uploadFile(projectId, file);
        String originalFilename = file.getOriginalFilename();

        PM_FileNode fileNode = new PM_FileNode();
        fileNode.setProjectId(projectId);
        fileNode.setParentNodeId(parentNodeId);
        fileNode.setName(removeExtension(originalFilename));
        fileNode.setType(NodeType.STATIC_FILE);
        fileNode.setExtension(getFileExtension(originalFilename));
        fileNode.setSizeBytes(file.getSize());
        fileNode.setStorageReference(storageRef);
        fileNode.setCreatedByUserId(userId);

        PM_FileNode savedFile = fileNodeRepository.save(fileNode);
        return new BaseResponse<>(1, "File uploaded successfully", savedFile);
    }

    @Override
    @Transactional
    public BaseResponse<?> moveNode(Long userId, Long projectId, Long nodeId, Long newParentId) {
        log.info("User {} moving node {} to parent {} in project {}", userId, nodeId, newParentId, projectId);

        // 1. Authorization: User must be an active member of the project
        authHelper.requireActiveMember(projectId, userId);

        // 2. Fetch the node to be moved
        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("File or folder not found"));

        // 3. Project Validation: Ensure the node belongs to this project
        if (!node.getProjectId().equals(projectId)) {
            return new BaseResponse<>(0, "This item does not belong to the specified project", null);
        }

        // 4. Destination Validation
        if (newParentId != null) {
            // Prevent moving a node into itself
            if (nodeId.equals(newParentId)) {
                return new BaseResponse<>(0, "Cannot move a folder into itself", null);
            }

            PM_FileNode newParent = fileNodeRepository.findById(newParentId)
                    .orElseThrow(() -> new NotFoundException("Destination folder not found"));

            // Ensure the destination is in the same project
            if (!newParent.getProjectId().equals(projectId)) {
                return new BaseResponse<>(0, "Destination folder belongs to a different project", null);
            }

            // Ensure destination is actually a folder
            if (newParent.getType() != NodeType.FOLDER) {
                return new BaseResponse<>(0, "Destination must be a folder", null);
            }

            // Circular Reference Check: Prevent moving a folder into its own subfolders
            if (isDescendant(nodeId, newParentId)) {
                return new BaseResponse<>(0, "Cannot move a folder into its own subfolder", null);
            }
        }

        // 5. Update and Persist
        node.setParentNodeId(newParentId);
        node.setUpdatedAt(java.time.LocalDateTime.now());
        fileNodeRepository.save(node);

        log.info("Node {} successfully moved to parent {}", nodeId, newParentId);
        return new BaseResponse<>(1, "Item moved successfully", null);
    }

    /**
     * Helper method to prevent circular references.
     * Checks if the 'potentialParentId' is a descendant of 'targetNodeId'.
     */
    private boolean isDescendant(Long targetNodeId, Long potentialParentId) {
        Long currentId = potentialParentId;
        while (currentId != null) {
            PM_FileNode current = fileNodeRepository.findById(currentId).orElse(null);
            if (current == null) break;

            // If we encounter targetNodeId while traversing up, it's a circular reference
            if (current.getParentNodeId() != null && current.getParentNodeId().equals(targetNodeId)) {
                return true;
            }
            currentId = current.getParentNodeId();
        }
        return false;
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteNode(Long userId, Long projectId, Long nodeId) {
        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("File/Folder not found"));

        if (!node.getProjectId().equals(projectId)) {
            return new BaseResponse<>(0, "This file does not belong to the specified project", null);
        }

        boolean isProjectOwner = authHelper.isOwner(projectId, userId);
        boolean isNodeCreator = node.getCreatedByUserId().equals(userId);

        if (!isProjectOwner && !isNodeCreator) {
            return new BaseResponse<>(0, "You do not have permission to delete this item.", null);
        }

        deleteNodeRecursive(node);

        return new BaseResponse<>(1, "Item deleted successfully", null);
    }

    @Override
    public BaseResponse<?> searchFiles(Long userId, Long projectId, String keyword) {
        authHelper.requireActiveMember(projectId, userId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return new BaseResponse<>(1, "Keyword is empty", List.of());
        }

        List<PM_FileNode> results = fileNodeRepository.findByProjectIdAndNameContainingIgnoreCase(projectId,
                keyword.trim());

        return new BaseResponse<>(1, "Search results retrieved", results);
    }

    // ============================================
    // New Methods for Notion Document
    // ============================================

    @Override
    @Transactional
    public BaseResponse<?> createNotionDocument(Long userId, Long projectId, Long parentNodeId, String name) {
        log.info("Creating Notion document '{}' in project {} by user {}", name, projectId, userId);

        authHelper.requireActiveMember(projectId, userId);

        // Validate parent node if provided
        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null)
                return new BaseResponse<>(0, error, null);
        }

        // Step 1: Create PM_FileNode entry first (to get nodeId)
        PM_FileNode fileNode = new PM_FileNode();
        fileNode.setProjectId(projectId);
        fileNode.setParentNodeId(parentNodeId);
        fileNode.setName(name);
        fileNode.setType(NodeType.NOTION_DOC);
        fileNode.setCreatedByUserId(userId);
        fileNode.setSizeBytes(0L);

        PM_FileNode savedNode = fileNodeRepository.save(fileNode);
        log.debug("Created PM_FileNode with id: {}", savedNode.getNodeId());

        // Step 2: Create empty document in MongoDB via Document Service
        Optional<String> storageRefOpt = documentServiceClient.createDocument(savedNode.getNodeId(), projectId);

        if (storageRefOpt.isEmpty()) {
            // Rollback: Delete the Postgres entry
            fileNodeRepository.delete(savedNode);
            log.error("Failed to create MongoDB document for nodeId: {}", savedNode.getNodeId());
            return new BaseResponse<>(0, "Failed to create document. Please try again.", null);
        }

        // Step 3: Update PM_FileNode with storage reference
        savedNode.setStorageReference(storageRefOpt.get());
        fileNodeRepository.save(savedNode);

        log.info("Notion document created successfully. NodeId: {}, StorageRef: {}",
                savedNode.getNodeId(), storageRefOpt.get());

        // Build response
        NotionDocDTO response = NotionDocDTO.builder()
                .nodeId(savedNode.getNodeId())
                .name(savedNode.getName())
                .storageReference(savedNode.getStorageReference())
                .createdAt(savedNode.getCreatedAt())
                .build();

        return new BaseResponse<>(1, "Page created", response);
    }

    @Override
    public BaseResponse<?> getDocumentDetails(Long userId, Long nodeId) {
        log.debug("Getting document details for nodeId: {} by user: {}", nodeId, userId);

        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        // Check user is member of the project
        authHelper.requireActiveMember(node.getProjectId(), userId);

        // Verify it's a Notion document
        if (node.getType() != NodeType.NOTION_DOC) {
            return new BaseResponse<>(0, "This is not a Notion document", null);
        }

        // Get user's role for FE to show/hide delete button - can delete later, for now, prioritize correctness
        PM_ProjectMember member = authHelper.getMember(node.getProjectId(), userId);
        String role = member.getRole().name();

        UserBatchDTO userBatchDTO = userServiceClient.findById(node.getCreatedByUserId())
                .orElseThrow(() -> new NotFoundException("Document creator not found"));

        NotionDocDTO response = NotionDocDTO.builder()
                .nodeId(node.getNodeId())
                .projectId(node.getProjectId()) // Add this for file picker
                .name(node.getName())
                .storageReference(node.getStorageReference())
                .role(role)
                .createdAt(node.getCreatedAt())
                .updatedAt(node.getUpdatedAt())
                .createdBy(userBatchDTO.getName())
                .build();

        return new BaseResponse<>(1, "Details loaded", response);
    }

    @Override
    public BaseResponse<?> getVersionHistory(Long userId, Long nodeId) {
        log.debug("Getting version history for nodeId: {} by user: {}", nodeId, userId);

        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        authHelper.requireActiveMember(node.getProjectId(), userId);

        if (node.getType() != NodeType.NOTION_DOC) {
            return new BaseResponse<>(0, "This is not a Notion document", null);
        }

        // Get version metadata from Postgres
        List<PM_DocVersion> versions = docVersionRepository.findByNodeIdOrderByCreatedAtDesc(nodeId);

        if (versions.isEmpty()) {
            return new BaseResponse<>(1, "No version history found", List.of());
        }

        // Batch fetch user info
        List<Long> userIds = versions.stream()
                .map(PM_DocVersion::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserBatchDTO> userMap = userServiceClient.findUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserBatchDTO::getUserId, Function.identity()));

        // Build response
        List<DocVersionDTO> versionDTOs = versions.stream()
                .map(v -> {
                    UserBatchDTO user = userMap.get(v.getCreatedBy());
                    return DocVersionDTO.builder()
                            .versionId(v.getVersionId())
                            .snapshotRef(v.getSnapshotRef())
                            .versionNumber(v.getVersionNumber())
                            .createdBy(v.getCreatedBy())
                            .createdByName(user != null ? user.getName() : "Unknown")
                            .createdByAvatar(user != null ? user.getAvatarUrl() : null)
                            .createdAt(v.getCreatedAt())
                            .reason(v.getReason())
                            .build();
                })
                .collect(Collectors.toList());

        return new BaseResponse<>(1, "Version history retrieved", versionDTOs);
    }

    @Override
    @Transactional
    public BaseResponse<?> restoreVersion(Long userId, Long nodeId, String snapShotRef) {
        log.info("Restoring version {} for nodeId: {} by user: {}", snapShotRef, nodeId, userId);

        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        authHelper.requireActiveMember(node.getProjectId(), userId);

        if (node.getType() != NodeType.NOTION_DOC) {
            return new BaseResponse<>(0, "This is not a Notion document", null);
        }

        // Find the version to restore
        PM_DocVersion versionToRestore = docVersionRepository.findBySnapshotRef(snapShotRef)
                .orElseThrow(() -> new NotFoundException("Version not found"));

        String storageRef = node.getStorageReference();

        // Step 1: Create a backup snapshot of current state before restoring
        Optional<Map<String, Object>> backupResult = documentServiceClient.createSnapshot(
                storageRef,
                Constant.REASON_BEFORE_RESTORE,
                userId
        );

        if (backupResult.isPresent()) {
            // Save backup version metadata to Postgres
            Integer nextVersionNumber = docVersionRepository.findMaxVersionNumberByNodeId(nodeId) + 1;
            PM_DocVersion backupVersion = PM_DocVersion.builder()
                    .nodeId(nodeId)
                    .snapshotRef(backupResult.get().get("snapshotId").toString())
                    .versionNumber(nextVersionNumber)
                    .createdBy(userId)
                    .reason(Constant.REASON_BEFORE_RESTORE)
                    .build();
            docVersionRepository.save(backupVersion);
        }

        // ✅ ADD NEW STEP 2: Execute Restore
        boolean success = documentServiceClient.restoreToSnapshot(
                storageRef,
                versionToRestore.getSnapshotRef());

        if (!success) {
            return new BaseResponse<>(0, "Failed to restore document content in Document Service", null);
        }

        log.info("Version {} restored successfully for nodeId: {}", snapShotRef, nodeId);

        Map<String, Object> response = new HashMap<>();
        response.put("restoredVersionId", snapShotRef);
        response.put("restoredVersionNumber", versionToRestore.getVersionNumber());
        response.put("message", "Version restored. Please refresh the page.");

        return new BaseResponse<>(1, "Version restored successfully", response);
    }

    @Override
    public BaseResponse<?> validateDocumentAccess(Long userId, String storageRef) {
        log.debug("Validating document access for user: {}, storageRef: {}", userId, storageRef);

        // Find document by storage reference
        Optional<PM_FileNode> nodeOpt = fileNodeRepository.findByStorageReference(storageRef);

        if (nodeOpt.isEmpty()) {
            return new BaseResponse<>(0, "Document not found", null);
        }

        PM_FileNode node = nodeOpt.get();

        // Check user is member of the project
        try {
            PM_ProjectMember member = authHelper.getMember(node.getProjectId(), userId);

            if (member.getRole() == ProjectMembershipRole.INVITED) {
                return new BaseResponse<>(0, "Please accept invitation first", null);
            }

            // Get user info for presence feature
            List<UserBatchDTO> users = userServiceClient.findUsersByIds(List.of(userId));
            UserBatchDTO user = users.isEmpty() ? null : users.get(0);

            DocumentAccessDTO accessDTO = DocumentAccessDTO.builder()
                    .nodeId(node.getNodeId())
                    .projectId(node.getProjectId())
                    .role(member.getRole().name())
                    .canEdit(true) // Both OWNER and MEMBER can edit
                    .canDelete(member.getRole() == ProjectMembershipRole.OWNER)
                    .userId(userId)
                    .userName(user != null ? user.getName() : "Unknown")
                    .userAvatar(user != null ? user.getAvatarUrl() : null)
                    .build();

            return new BaseResponse<>(1, "Access granted", accessDTO);

        } catch (NotFoundException | ForbiddenException e) {
            return new BaseResponse<>(0, e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDocument(Long userId, Long nodeId, String name) {
        // 1. Fetch Node
        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        // 2. Check Permission (Must be an active member)
        authHelper.requireActiveMember(node.getProjectId(), userId);

        // 3. Update Name
        if (name != null && !name.trim().isEmpty()) {
            node.setName(name.trim());
            fileNodeRepository.save(node);
        }

        // 4. Return Simple Success
        return new BaseResponse<>(1, "Title updated successfully", null);
    }

    // Add to Interface first:
    // void syncVersion(Long nodeId, String snapshotRef, Long userId, String
    // reason);

    @Override
    @Transactional
    public BaseResponse<?> syncVersion(Long nodeId, String snapshotRef, Long userId, String reason) {
        // 1. Verify Node exists
        if (!fileNodeRepository.existsById(nodeId)) {
            log.warn("Cannot sync version: Node {} does not exist", nodeId);
            return new BaseResponse<>(0, "Node does not exist", null);
        }

        // 2. Calculate next Version Number (1, 2, 3...)
        Integer maxVersion = docVersionRepository.findMaxVersionNumberByNodeId(nodeId);
        int nextVersion = (maxVersion == null) ? 1 : maxVersion + 1;

        // 3. Save to SQL Table (pm_doc_version)
        PM_DocVersion newVersion = PM_DocVersion.builder()
                .nodeId(nodeId)
                .snapshotRef(snapshotRef) // "693d..."
                .versionNumber(nextVersion)
                .createdBy(userId)
                .reason(reason != null ? reason : "AUTO_SAVE")
                .build();

        docVersionRepository.save(newVersion);
        log.info("Saved Version {} (Snapshot: {}) for Node {}", nextVersion, snapshotRef, nodeId);

        return new BaseResponse<>(1, "Version synced successfully", null);
    }

    @Override
    public BaseResponse<?> uploadEditorImage(Long userId, Long projectId, MultipartFile file) throws IOException {
        log.info("Uploading editor image for project {} by user {}", projectId, userId);

        authHelper.requireActiveMember(projectId, userId);

        // Upload to separate GCS path for editor images
        String imageUrl = storageService.uploadEditorImage(projectId, file);

        Map<String, Object> response = new HashMap<>();
        response.put("url", imageUrl);
        response.put("filename", file.getOriginalFilename());

        return new BaseResponse<>(1, "Image uploaded successfully", response);
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private String validateParentNode(Long projectId, Long parentNodeId) {
        Optional<PM_FileNode> parentOpt = fileNodeRepository.findById(parentNodeId);

        if (parentOpt.isEmpty()) {
            return "Parent folder not found";
        }

        PM_FileNode parent = parentOpt.get();

        if (!parent.getProjectId().equals(projectId)) {
            return "Invalid parent folder: It belongs to a different project";
        }

        if (parent.getType() != NodeType.FOLDER) {
            return "Invalid parent: Cannot create a file inside another file";
        }

        return null;
    }

    private void deleteNodeRecursive(PM_FileNode node) {
        if (node.getType() == NodeType.FOLDER) {
            List<PM_FileNode> children = fileNodeRepository.findByParentNodeId(node.getNodeId());
            for (PM_FileNode child : children) {
                deleteNodeRecursive(child);
            }
        }

        if (node.getType() == NodeType.STATIC_FILE && node.getStorageReference() != null) {
            storageService.deleteFile(node.getStorageReference());
        }

        if (node.getType() == NodeType.NOTION_DOC && node.getStorageReference() != null) {
            // Delete MongoDB document and snapshots
            documentServiceClient.deleteDocument(node.getStorageReference());
            // Delete version metadata from Postgres
            docVersionRepository.deleteByNodeId(node.getNodeId());
        }

        fileNodeRepository.delete(node);
    }

    private String removeExtension(String filename) {
        if (filename == null || filename.isEmpty())
            return filename;
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, lastDotIndex);
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.lastIndexOf(".") != -1
                ? filename.substring(filename.lastIndexOf(".") + 1)
                : "";
    }
}