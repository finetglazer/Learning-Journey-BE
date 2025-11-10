package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseRepository extends JpaRepository<PM_Phase, Long> {

    List<PM_Phase> findByDeliverableIdOrderByOrderAsc(Long deliverableId);

    @Query("SELECT COALESCE(MAX(p.order), -1) FROM PM_Phase p WHERE p.deliverableId = :deliverableId")
    Integer findMaxOrderByDeliverableId(@Param("deliverableId") Long deliverableId);

    @Query("SELECT p.deliverableId FROM PM_Phase p WHERE p.phaseId = :phaseId")
    Long findDeliverableIdByPhaseId(@Param("phaseId") Long phaseId);
}