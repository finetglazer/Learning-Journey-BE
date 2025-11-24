package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependencyRepository extends JpaRepository<PM_Dependency, Long> {

    // Used for DELETE and validation
    Optional<PM_Dependency> findByProjectIdAndTypeAndSourceIdAndTargetId(
            Long projectId, String type, Long sourceId, Long targetId
    );

    // Used for GET (Lazy load)
    // Fetches any dependency where the item is either the source OR the target
    @Query("SELECT d FROM PM_Dependency d " +
            "WHERE d.projectId = :projectId " +
            "AND d.type = :type " +
            "AND (d.sourceId = :itemId OR d.targetId = :itemId)")
    List<PM_Dependency> findAllRelatedDependencies(
            @Param("projectId") Long projectId,
            @Param("type") String type,
            @Param("itemId") Long itemId
    );
}