package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_FileNode;
import com.graduation.projectservice.model.enums.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileNodeRepository extends JpaRepository<PM_FileNode, Long> {

    // Find children of a specific folder
    List<PM_FileNode> findByProjectIdAndParentNodeId(Long projectId, Long parentNodeId);

    // Find root items (where parent is null)
    List<PM_FileNode> findByProjectIdAndParentNodeIdIsNull(Long projectId);

    // For recursive deletion logic
    List<PM_FileNode> findByParentNodeId(Long parentNodeId);

    // For search functionality
    List<PM_FileNode> findByProjectIdAndNameContainingIgnoreCase(Long projectId, String name);

    // Find by storage reference (for WebSocket auth)
    Optional<PM_FileNode> findByStorageReference(String storageReference);

    // Find by storage reference and type
    Optional<PM_FileNode> findByStorageReferenceAndType(String storageReference, NodeType type);
}