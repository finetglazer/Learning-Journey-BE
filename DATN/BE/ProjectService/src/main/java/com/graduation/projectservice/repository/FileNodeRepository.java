package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_FileNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileNodeRepository extends JpaRepository<PM_FileNode, Long> {
    // Find children of a specific folder
    List<PM_FileNode> findByProjectIdAndParentNodeId(Long projectId, Long parentNodeId);

    // Find root items (where parent is null)
    List<PM_FileNode> findByProjectIdAndParentNodeIdIsNull(Long projectId);

    // For recursive deletion logic
    List<PM_FileNode> findByParentNodeId(Long parentNodeId);

    List<PM_FileNode> findByProjectIdAndNameContainingIgnoreCase(Long projectId, String name);
}