package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMembershipRole;
import com.graduation.projectservice.payload.request.AcceptInvitationRequest;
import com.graduation.projectservice.payload.request.AddMemberRequest;
import com.graduation.projectservice.payload.request.DeclineInvitationRequest;
import com.graduation.projectservice.payload.request.UpdateMemberRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.MemberResponse;
import com.graduation.projectservice.payload.response.UserBatchDTO;
import com.graduation.projectservice.repository.ProjectMemberRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserServiceClient userServiceClient;
    private final ProjectAuthorizationHelper authHelper;

    @Override
    @Transactional
    public BaseResponse<?> addMember(Long userId, Long projectId, AddMemberRequest request) {
        try {
            // 1. Authorization: Only active member can invite other members
            authHelper.requireActiveMember(projectId, userId);

            // 2. Verify project exists
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // 3. Find user by email from UserService
            UserBatchDTO userToInvite = userServiceClient.findUserByEmail(request.getEmail())
                    .orElse(null);

            if (userToInvite == null) {
                return new BaseResponse<>(0, "No user found with that email address.", 
                        Map.of("error_code", "USER_NOT_FOUND"));
            }

            // 4. Check if user is already a member
            if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userToInvite.getUserId())) {
                return new BaseResponse<>(0, "User is already a member of this project", null);
            }

            // 5. Create PM_ProjectMember with role INVITED
            PM_ProjectMember member = new PM_ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(userToInvite.getUserId());
            member.setRole(ProjectMembershipRole.INVITED);
            member.setCustomRoleName(null);
            
            projectMemberRepository.save(member);

            // 6. Send invitation email via UserService
            userServiceClient.sendInvitation(userToInvite.getUserId(), projectId, project.getName());

            // 7. Build response
            MemberResponse response = MemberResponse.builder()
                    .userId(userToInvite.getUserId())
                    .name(userToInvite.getName())
                    .avatarUrl(userToInvite.getAvatarUrl())
                    .email(userToInvite.getEmail())
                    .role(ProjectMembershipRole.INVITED)
                    .customRoleName(null)
                    .build();

            Map<String, Object> data = new HashMap<>();
            data.put("member", response);

            log.info("User {} invited user {} to project {}", userId, userToInvite.getUserId(), projectId);
            return new BaseResponse<>(1, "User invited to project", data);

        } catch (Exception e) {
            log.error("Failed to add member to project {}: {}", projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to add member: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getMembers(Long userId, Long projectId) {
        try {
            // 1. Authorization: Any member can view member list
            authHelper.requireMember(projectId, userId);

            // 2. Get all project members
            List<PM_ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);

            // 3. Extract user IDs
            List<Long> userIds = members.stream()
                    .map(PM_ProjectMember::getUserId)
                    .collect(Collectors.toList());

            // 4. Batch fetch user details from UserService
            List<UserBatchDTO> users = userServiceClient.findUsersByIds(userIds);

            // 5. Create map for efficient lookup
            Map<Long, UserBatchDTO> userMap = users.stream()
                    .collect(Collectors.toMap(UserBatchDTO::getUserId, u -> u));

            // 6. Build member responses
            List<MemberResponse> memberResponses = members.stream()
                    .map(member -> {
                        UserBatchDTO user = userMap.get(member.getUserId());
                        return MemberResponse.builder()
                                .userId(member.getUserId())
                                .name(user != null ? user.getName() : "Unknown")
                                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                                .email(user != null ? user.getEmail() : "unknown@example.com")
                                .role(member.getRole())
                                .customRoleName(member.getCustomRoleName())
                                .build();
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("members", memberResponses);

            log.info("User {} retrieved {} members for project {}", userId, memberResponses.size(), projectId);
            return new BaseResponse<>(1, "Project members retrieved successfully", data);

        } catch (Exception e) {
            log.error("Failed to get members for project {}: {}", projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to retrieve members: " + e.getMessage(), null);
        }
    }

    @Override
    public BaseResponse<?> findUsersByEmail(Long projectId, Long userId, String email) {
        log.info(Constant.LOG_FINDING_USERS_REQUEST, userId);

        // 1. Get list of ALL users that match the email from the User Service
        List<UserBatchDTO> allFoundUsers = userServiceClient.findUsersByEmail(email);

        // 2. Remove users who are currently members of THIS project
        List<UserBatchDTO> nonMembers = allFoundUsers.stream()
                .filter(user -> !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getUserId()))
                .toList();

        // 3. Return the filtered list
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.FINDING_USERS_SUCCESS,
                nonMembers
        );
    }

    @Override
    @Transactional
    public BaseResponse<?> updateMember(Long userId, Long projectId, Long targetUserId, UpdateMemberRequest request) {
        try {
            // 1. Authorization: Only OWNER can update members
            authHelper.requireOwner(projectId, userId);

            // 2. Find the member to update
            PM_ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            // 3. Cannot modify owner's role
            if (member.getRole() == ProjectMembershipRole.OWNER) {
                return new BaseResponse<>(0, "Cannot modify project owner", null);
            }

            // 4. Update custom role name
            member.setCustomRoleName(request.getCustomRoleName());
            projectMemberRepository.save(member);

            // 5. Get user details for response
//            UserBatchDTO user = userServiceClient.findUsersByIds(List.of(targetUserId))
//                    .stream()
//                    .findFirst()
//                    .orElse(null);

            MemberResponse response = MemberResponse.builder()
                    .userId(member.getUserId())
//                    .name(user != null ? user.getName() : "Unknown")
//                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
//                    .email(user != null ? user.getEmail() : "unknown@example.com")
                    .role(member.getRole())
                    .customRoleName(member.getCustomRoleName())
                    .build();

            Map<String, Object> data = new HashMap<>();
            data.put("member", response);

            log.info("User {} updated member {} in project {}", userId, targetUserId, projectId);
            return new BaseResponse<>(1, "Member updated", data);

        } catch (Exception e) {
            log.error("Failed to update member {} in project {}: {}", targetUserId, projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to update member: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> removeMember(Long userId, Long projectId, Long targetUserId) {
        try {
            // 1. Authorization: Only OWNER can remove members
            authHelper.requireOwner(projectId, userId);

            // 2. Find the member to remove
            PM_ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            // 3. Cannot remove the owner
            if (member.getRole() == ProjectMembershipRole.OWNER) {
                return new BaseResponse<>(0, "Cannot remove project owner", null);
            }

            // 4. Delete the member
            projectMemberRepository.deleteByProjectIdAndUserId(projectId, targetUserId);

            log.info("User {} removed member {} from project {}", userId, targetUserId, projectId);
            return new BaseResponse<>(1, "User removed from project", Map.of());

        } catch (Exception e) {
            log.error("Failed to remove member {} from project {}: {}", targetUserId, projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to remove member: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> acceptInvitation(Long projectId, AcceptInvitationRequest request) {
        try {
            // 1. Validate token with UserService and get userId/projectId
            Map<String, Long> tokenData = userServiceClient.validateInvitationToken(request.getToken());

            if (tokenData == null) {
                return new BaseResponse<>(0, "Invalid or expired invitation token",
                        Map.of("error_code", "INVALID_TOKEN"));
            }

            Long userId = tokenData.get("userId");
            Long tokenProjectId = tokenData.get("projectId");

            // 2. Verify projectId matches
            if (!tokenProjectId.equals(projectId)) {
                return new BaseResponse<>(0, "Token does not match this project",
                        Map.of("error_code", "PROJECT_MISMATCH"));
            }

            // 3. Find the member
            PM_ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            // 4. Check if already accepted
            if (member.getRole() == ProjectMembershipRole.MEMBER) {
                return new BaseResponse<>(1, "You are already a member of this project", Map.of());
            }

            // 5. Verify status is INVITED
            if (member.getRole() != ProjectMembershipRole.INVITED) {
                return new BaseResponse<>(0, "Invalid invitation status", null);
            }

            // 6. Change role from INVITED to MEMBER
            member.setRole(ProjectMembershipRole.MEMBER);
            projectMemberRepository.save(member);

            log.info("User {} accepted invitation to project {}", userId, projectId);
            return new BaseResponse<>(1, "Invitation accepted successfully", Map.of());

        } catch (Exception e) {
            log.error("Failed to accept invitation to project {}: {}", projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to accept invitation: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> declineInvitation(Long projectId, DeclineInvitationRequest request) {
        try {
            // 1. Validate token with UserService and get userId/projectId
            Map<String, Long> tokenData = userServiceClient.validateInvitationToken(request.getToken());

            if (tokenData == null) {
                return new BaseResponse<>(0, "Invalid or expired invitation token",
                        Map.of("error_code", "INVALID_TOKEN"));
            }

            Long userId = tokenData.get("userId");
            Long tokenProjectId = tokenData.get("projectId");

            // 2. Verify projectId matches
            if (!tokenProjectId.equals(projectId)) {
                return new BaseResponse<>(0, "Token does not match this project",
                        Map.of("error_code", "PROJECT_MISMATCH"));
            }

            // 3. Find the member record
            PM_ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            // 4. Verify status is INVITED
            if (member.getRole() != ProjectMembershipRole.INVITED) {
                return new BaseResponse<>(0, "No pending invitation found", null);
            }

            // 5. Delete the member record
            projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);

            log.info("User {} declined invitation to project {}", userId, projectId);
            return new BaseResponse<>(1, "Invitation declined successfully", Map.of());

        } catch (Exception e) {
            log.error("Failed to decline invitation to project {}: {}", projectId, e.getMessage(), e);
            return new BaseResponse<>(0, "Failed to decline invitation: " + e.getMessage(), null);
        }
    }
}
