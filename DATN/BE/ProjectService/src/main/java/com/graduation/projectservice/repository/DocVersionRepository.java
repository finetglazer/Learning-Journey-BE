package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_DocVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocVersionRepository extends JpaRepository<PM_DocVersion, Long> {

    List<PM_DocVersion> findByNodeIdOrderByCreatedAtDesc(Long nodeId);

    Optional<PM_DocVersion> findByNodeIdAndVersionId(Long nodeId, Long versionId);

    Optional<PM_DocVersion> findBySnapshotRef(String snapshotRef);

    @Query("SELECT COALESCE(MAX(d.versionNumber), 0) FROM PM_DocVersion d WHERE d.nodeId = :nodeId")
    Integer findMaxVersionNumberByNodeId(@Param("nodeId") Long nodeId);

    long countByNodeId(Long nodeId);

    void deleteByNodeId(Long nodeId);

    Optional<PM_DocVersion> findFirstByNodeIdOrderByCreatedAtDesc(Long nodeId);
}
