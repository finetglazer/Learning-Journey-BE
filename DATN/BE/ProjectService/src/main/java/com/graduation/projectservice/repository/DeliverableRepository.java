package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliverableRepository extends JpaRepository<PM_Deliverable, Long> {

    List<PM_Deliverable> findByProjectIdOrderByOrderAsc(Long projectId);

    @Query("SELECT COALESCE(MAX(d.order), -1) FROM PM_Deliverable d WHERE d.projectId = :projectId")
    Integer findMaxOrderByProjectId(@Param("projectId") Long projectId);

    List<PM_Deliverable> findAllByProjectIdOrderByOrderAsc(Long phaseId);
}