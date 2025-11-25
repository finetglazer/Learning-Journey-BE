package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_TaskAssignee;
import com.graduation.projectservice.model.PM_TaskAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<PM_TaskAssignee, PM_TaskAssigneeId> {

    List<PM_TaskAssignee> findByTaskId(Long taskId);

    @Modifying
    @Query("DELETE FROM PM_TaskAssignee ta WHERE ta.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT ta.userId, COUNT(ta.taskId) " +
            "FROM PM_TaskAssignee ta " +
            "JOIN PM_Task t ON ta.taskId = t.taskId " +
            "JOIN PM_Phase p ON t.phaseId = p.phaseId " +
            "JOIN PM_Deliverable d ON p.deliverableId = d.deliverableId " +
            "WHERE d.projectId = :projectId " +
            "GROUP BY ta.userId")
    List<Object[]> countTasksByUserInProject(@Param("projectId") Long projectId);
}