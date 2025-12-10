package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.exception.NotFoundException;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_FileNode;
import com.graduation.projectservice.model.PM_Task;
import com.graduation.projectservice.model.PM_TaskAttachment;
import com.graduation.projectservice.model.enums.NodeType;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.TaskAttachmentDetailDTO;
import com.graduation.projectservice.repository.FileNodeRepository;
import com.graduation.projectservice.service.FileNodeService;
import com.graduation.projectservice.service.ProjectFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileNodeServiceImpl implements FileNodeService {

    private final FileNodeRepository fileNodeRepository;
    private final ProjectFileStorageService storageService;
    private final ProjectAuthorizationHelper authHelper;

    @Override
    public BaseResponse<?> getFiles(Long userId, Long projectId, Long parentNodeId) {
        authHelper.requireActiveMember(projectId, userId);

        // Optional: Validate parent exists if provided
        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null) return new BaseResponse<>(0, error, null);
        }

        List<PM_FileNode> files;
        if (parentNodeId == null) {
            files = fileNodeRepository.findByProjectIdAndParentNodeIdIsNull(projectId);
        } else {
            files = fileNodeRepository.findByProjectIdAndParentNodeId(projectId, parentNodeId);
        }

        return new BaseResponse<>(1, "Files retrieved successfully", files);
    }

    @Override
    @Transactional
    public BaseResponse<?> createFolder(Long userId, Long projectId, Long parentNodeId, String name) {
        authHelper.requireActiveMember(projectId, userId);

        // 1. STRICT VALIDATION: Check if Parent Node is valid
        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null) return new BaseResponse<>(0, error, null);
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
    public BaseResponse<?> uploadFile(Long userId, Long projectId, Long parentNodeId, MultipartFile file) throws IOException {
        authHelper.requireActiveMember(projectId, userId);

        // 1. STRICT VALIDATION: Check if Parent Node is valid
        if (parentNodeId != null) {
            String error = validateParentNode(projectId, parentNodeId);
            if (error != null) return new BaseResponse<>(0, error, null);
        }

        // Upload to Cloud (This logic remains untouched to ensure file integrity)
        String storageRef = storageService.uploadFile(projectId, file);

        // Prepare Database Entity
        String originalFilename = file.getOriginalFilename();

        PM_FileNode fileNode = new PM_FileNode();
        fileNode.setProjectId(projectId);
        fileNode.setParentNodeId(parentNodeId);

        // --- CHANGE HERE: Strip the extension for the display name ---
        fileNode.setName(removeExtension(originalFilename));
        // -----------------------------------------------------------

        fileNode.setType(NodeType.STATIC_FILE);

        // We still save the extension separately so the UI knows what icon to show (PDF, IMG, etc.)
        fileNode.setExtension(getFileExtension(originalFilename));

        fileNode.setSizeBytes(file.getSize());
        fileNode.setStorageReference(storageRef);
        fileNode.setCreatedByUserId(userId);

        PM_FileNode savedFile = fileNodeRepository.save(fileNode);
        return new BaseResponse<>(1, "File uploaded successfully", savedFile);
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteNode(Long userId, Long projectId, Long nodeId) {
        // 1. Fetch Node
        PM_FileNode node = fileNodeRepository.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("File/Folder not found"));

        // 2. STRICT VALIDATION: Ensure Node belongs to the Request's Project
        // This prevents deleting a file from Project B using Project A's API
        if (!node.getProjectId().equals(projectId)) {
            return new BaseResponse<>(0, "This file does not belong to the specified project", null);
        }

        // 3. Check Permissions (Owner or Creator)
        boolean isProjectOwner = authHelper.isOwner(projectId, userId);
        boolean isNodeCreator = node.getCreatedByUserId().equals(userId);

        if (!isProjectOwner && !isNodeCreator) {
            return new BaseResponse<>(0, "You do not have permission to delete this item.", null);
        }

        // 4. Recursive Delete
        deleteNodeRecursive(node);

        return new BaseResponse<>(1, "Item deleted successfully", null);
    }

    @Override
    public BaseResponse<?> searchFiles(Long userId, Long projectId, String keyword) {
        // 1. Check permissions
        authHelper.requireActiveMember(projectId, userId);

        // 2. Validate input
        if (keyword == null || keyword.trim().isEmpty()) {
            return new BaseResponse<>(1, "Keyword is empty", List.of());
        }

        // 3. Perform Search
        // We trim the keyword to remove accidental spaces
        List<PM_FileNode> results = fileNodeRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, keyword.trim());

        return new BaseResponse<>(1, "Search results retrieved", results);
    }


    // --- Private Helper Methods ---

    /**
     * Validates that the parent node:
     * 1. Exists
     * 2. Belongs to the same project (Prevent ID spoofing)
     * 3. Is actually a FOLDER (Cannot nest inside a file)
     * Returns error message string, or NULL if valid.
     */
    private String validateParentNode(Long projectId, Long parentNodeId) {
        Optional<PM_FileNode> parentOpt = fileNodeRepository.findById(parentNodeId);

        if (parentOpt.isEmpty()) {
            return "Parent folder not found";
        }

        PM_FileNode parent = parentOpt.get();

        // Check Context Integrity
        if (!parent.getProjectId().equals(projectId)) {
            return "Invalid parent folder: It belongs to a different project";
        }

        // Check Type Integrity
        if (parent.getType() != NodeType.FOLDER) {
            return "Invalid parent: Cannot create a file inside another file";
        }

        return null; // Valid
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
        fileNodeRepository.delete(node);
    }

    /**
     * Removes the extension from the filename.
     * Example: "report.final.pdf" -> "report.final"
     */
    private String removeExtension(String filename) {
        if (filename == null || filename.isEmpty()) return filename;

        int lastDotIndex = filename.lastIndexOf(".");

        // If no dot is found (e.g. "README"), or dot is the first char (hidden file ".config"), return as is
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