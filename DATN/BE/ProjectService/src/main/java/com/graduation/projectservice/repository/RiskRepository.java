package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Risk;
import com.graduation.projectservice.model.enums.RiskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskRepository extends JpaRepository<PM_Risk, Long> {

    @Query("SELECT r FROM PM_Risk r WHERE r.projectId = :projectId " +
            "AND (:keyword IS NULL OR LOWER(r.riskStatement) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.key) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:assigneeId IS NULL OR EXISTS (SELECT a FROM r.assignees a WHERE a.userId = :assigneeId))")
    Page<PM_Risk> searchRisks(@Param("projectId") Long projectId,
                              @Param("keyword") String keyword,
                              @Param("assigneeId") Long assigneeId,
                              Pageable pageable);
    List<PM_Risk> findByProjectIdAndStatus(Long projectId, RiskStatus status);
}