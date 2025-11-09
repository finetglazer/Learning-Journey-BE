package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.AcceptInvitationRequest;
import com.graduation.projectservice.payload.request.AddMemberRequest;
import com.graduation.projectservice.payload.request.DeclineInvitationRequest;
import com.graduation.projectservice.payload.request.UpdateMemberRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface ProjectMemberService {

    /**
     * Add a new member to the project by email (role = INVITED)
     */
    BaseResponse<?> addMember(Long userId, Long projectId, AddMemberRequest request);

    /**
     * Get all members of a project
     */
    BaseResponse<?> getMembers(Long userId, Long projectId);

    /**
     * Update member's custom role name
     */
    BaseResponse<?> updateMember(Long userId, Long projectId, Long targetUserId, UpdateMemberRequest request);

    /**
     * Remove a member from the project
     */
    BaseResponse<?> removeMember(Long userId, Long projectId, Long targetUserId);

    /**
     * Accept invitation and change role from INVITED to MEMBER
     */
    BaseResponse<?> acceptInvitation(Long projectId, AcceptInvitationRequest request);

    BaseResponse<?> declineInvitation(Long projectId, DeclineInvitationRequest request);
}
