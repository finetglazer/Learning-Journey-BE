package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<PM_Project, Long> {

    @Query("SELECT p FROM PM_Project p " +
            "INNER JOIN PM_ProjectMember pm ON p.projectId = pm.projectId " +
            "WHERE pm.userId = :userId")
    List<PM_Project> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM PM_Project p " +
            "INNER JOIN PM_ProjectMember pm ON p.projectId = pm.projectId " +
            "WHERE pm.userId = :userId AND pm.role <> 'INVITED'")
    List<PM_Project> findActiveProjectsByUserId(@Param("userId") Long userId);
}