package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMemberKey;
import com.graduation.projectservice.model.ProjectMembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<PM_ProjectMember, ProjectMemberKey> {

    List<PM_ProjectMember> findAllByProjectId(Long projectId);

    Optional<PM_ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, ProjectMembershipRole role);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}