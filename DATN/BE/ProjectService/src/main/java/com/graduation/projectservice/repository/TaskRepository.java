package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<PM_Task, Long> {

    List<PM_Task> findByPhaseIdOrderByOrderAsc(Long phaseId);

    @Query("SELECT COALESCE(MAX(t.order), -1) FROM PM_Task t WHERE t.phaseId = :phaseId")
    Integer findMaxOrderByPhaseId(@Param("phaseId") Long phaseId);

    @Query("SELECT t.phaseId FROM PM_Task t WHERE t.taskId = :taskId")
    Long findPhaseIdByTaskId(@Param("taskId") Long taskId);

}