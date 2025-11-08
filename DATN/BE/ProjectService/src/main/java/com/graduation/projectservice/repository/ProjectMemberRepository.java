package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<PM_ProjectMember, ProjectMemberKey> {
}