package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<PM_Milestone, Long> {
    List<PM_Milestone> findByProjectId(Long projectId);

    List<PM_Milestone> findAllByProjectIdOrderByDateAsc(Long projectId);
}