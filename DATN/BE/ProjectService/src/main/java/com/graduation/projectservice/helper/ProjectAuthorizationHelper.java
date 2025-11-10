package com.graduation.projectservice.helper;

import com.graduation.projectservice.exception.ForbiddenException;
import com.graduation.projectservice.exception.NotFoundException;
import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMembershipRole;
import com.graduation.projectservice.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectAuthorizationHelper {

    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Check if user is project OWNER
     * @throws ForbiddenException if user is not owner
     */
    public void requireOwner(Long projectId, Long userId) {
        PM_ProjectMember member = getMember(projectId, userId);
        
        if (member.getRole() != ProjectMembershipRole.OWNER) {
            log.warn("User {} attempted owner-only action on project {} with role {}", 
                    userId, projectId, member.getRole());
            throw new ForbiddenException("Only project owner can perform this action");
        }
    }

    /**
     * Check if user is any member (OWNER, MEMBER, or INVITED)
     * @throws ForbiddenException if user is not a member
     */
    public void requireMember(Long projectId, Long userId) {
        if (!isMember(projectId, userId)) {
            log.warn("User {} attempted to access project {} without membership", userId, projectId);
            throw new ForbiddenException("You are not a member of this project");
        }
    }

    /**
     * Check if user is active member (OWNER or MEMBER, not INVITED)
     * @throws ForbiddenException if user is only invited
     */
    public void requireActiveMember(Long projectId, Long userId) {
        PM_ProjectMember member = getMember(projectId, userId);
        
        if (member.getRole() == ProjectMembershipRole.INVITED) {
            log.warn("User {} attempted action on project {} with INVITED status", userId, projectId);
            throw new ForbiddenException("Please accept invitation first");
        }
    }

    /**
     * Get member or throw NotFoundException
     */
    public PM_ProjectMember getMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("You are not a member of this project"));
    }

    /**
     * Check if user is any member
     */
    public boolean isMember(Long projectId, Long userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    /**
     * Check if user is OWNER
     */
    public boolean isOwner(Long projectId, Long userId) {
        return projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                projectId, userId, ProjectMembershipRole.OWNER);
    }
}
