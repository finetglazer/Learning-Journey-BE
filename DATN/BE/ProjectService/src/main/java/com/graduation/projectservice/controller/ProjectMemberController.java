package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.AcceptInvitationRequest;
import com.graduation.projectservice.payload.request.AddMemberRequest;
import com.graduation.projectservice.payload.request.DeclineInvitationRequest;
import com.graduation.projectservice.payload.request.UpdateMemberRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping
    public ResponseEntity<BaseResponse<?>> addMember(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody AddMemberRequest request) {

        log.info("POST /members - User {} adding member to project {}", userId, projectId);

        BaseResponse<?> response = projectMemberService.addMember(userId, projectId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<?>> getMembers(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId) {

        log.info("GET /members - User {} retrieving members of project {}", userId, projectId);

        BaseResponse<?> response = projectMemberService.getMembers(userId, projectId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/find-users-by-email/{email}")
    public ResponseEntity<BaseResponse<?>> findUsersByEmail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable String email
    ) {
        log.info(Constant.LOG_FIND_USERS_REQUEST, userId);

        BaseResponse<?> response = projectMemberService.findUsersByEmail(projectId, userId, email);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{targetUserId}")
    public ResponseEntity<BaseResponse<?>> updateMember(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateMemberRequest request) {

        log.info("PUT /members/{} - User {} updating member in project {}", targetUserId, userId, projectId);

        BaseResponse<?> response = projectMemberService.updateMember(userId, projectId, targetUserId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<BaseResponse<?>> removeMember(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long targetUserId) {

        log.info("DELETE /members/{} - User {} removing member from project {}", targetUserId, userId, projectId);

        BaseResponse<?> response = projectMemberService.removeMember(userId, projectId, targetUserId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<BaseResponse<?>> acceptInvitation(
            @PathVariable Long projectId,
            @Valid @RequestBody AcceptInvitationRequest request) {

        log.info("POST /members/accept - Accepting invitation to project {}", projectId);

        BaseResponse<?> response = projectMemberService.acceptInvitation(projectId, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/decline")
    public ResponseEntity<BaseResponse<?>> declineInvitation(
            @PathVariable Long projectId,
            @Valid @RequestBody DeclineInvitationRequest request) {

        log.info("POST /members/decline - Declining invitation to project {}", projectId);

        BaseResponse<?> response = projectMemberService.declineInvitation(projectId, request);

        return ResponseEntity.ok(response);
    }
}
