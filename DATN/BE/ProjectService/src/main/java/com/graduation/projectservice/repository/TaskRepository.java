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

    @Query("SELECT t FROM PM_Task t JOIN PM_Phase p ON t.phaseId = p.phaseId JOIN PM_Deliverable d ON p.deliverableId = d.deliverableId WHERE d.projectId = :projectId ORDER BY t.order ASC")
    List<PM_Task> findAllTasksByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT t FROM PM_Task t " +
            "JOIN PM_Phase p ON t.phaseId = p.phaseId " +
            "JOIN PM_Deliverable d ON p.deliverableId = d.deliverableId " +
            "WHERE d.projectId = :projectId")
    List<PM_Task> findAllByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT " +
            "t.taskId as taskId, " +
            "t.name as taskName, " +
            "t.endDate as endDate, " +
            "p.projectId as projectId, " +
            "p.name as projectName " +
            "FROM PM_Task t " +
            "JOIN t.assignees a " +
            "JOIN PM_Phase ph ON t.phaseId = ph.phaseId " +
            "JOIN PM_Deliverable d ON ph.deliverableId = d.deliverableId " +
            "JOIN PM_Project p ON d.projectId = p.projectId " +
            "WHERE a.userId = :userId " +
            "AND t.status != 'DONE' " +
            "ORDER BY p.projectId ASC, t.endDate ASC")
    List<TaskProjectProjection> findActiveTasksForUser(@Param("userId") Long userId);
}