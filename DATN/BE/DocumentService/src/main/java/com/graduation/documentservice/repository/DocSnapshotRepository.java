package com.graduation.documentservice.repository;

import com.graduation.documentservice.model.DocSnapshot;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocSnapshotRepository extends MongoRepository<DocSnapshot, ObjectId> {

    List<DocSnapshot> findByPageIdOrderByCreatedAtDesc(ObjectId pageId);

    List<DocSnapshot> findByPgNodeIdOrderByCreatedAtDesc(Long pgNodeId);

    long countByPageId(ObjectId pageId);

    long countByPgNodeId(Long pgNodeId);

    void deleteByPageId(ObjectId pageId);

    void deleteByPgNodeId(Long pgNodeId);

    // For cleanup: find oldest snapshots beyond the max limit
    List<DocSnapshot> findByPgNodeIdOrderByCreatedAtAsc(Long pgNodeId);

    // For cleanup: find snapshots older than retention period
    List<DocSnapshot> findByCreatedAtBefore(LocalDateTime cutoffDate);

    Optional<DocSnapshot> findFirstByPgNodeIdOrderByCreatedAtDesc(Long pgNodeId);
}
