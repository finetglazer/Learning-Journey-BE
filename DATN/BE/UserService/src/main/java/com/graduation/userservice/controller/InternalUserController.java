package com.graduation.userservice.controller;

import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.payload.response.UserBatchDTO;
import com.graduation.userservice.service.InternalUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final InternalUserService internalUserService;

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        log.info("Internal API: Finding user by email: {}", email);

        return internalUserService.findUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/find-users-by-email/{email}")
    public ResponseEntity<?> getUsersByEmail(@PathVariable String email) {
        log.info("Internal API: Finding users by email: {}", email);

        return ResponseEntity.ok(internalUserService.findUsersByEmail(email));
    }

    @PostMapping("/batch-by-ids")
    public ResponseEntity<List<UserBatchDTO>> getUsersByIds(@RequestBody List<Long> userIds) {
        log.info("Internal API: Fetching {} users by IDs", userIds.size());

        List<UserBatchDTO> users = internalUserService.findUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/send-invitation")
    public ResponseEntity<?> sendInvitation(@RequestBody Map<String, Object> request) {
        Long userId = ((Number) request.get("userId")).longValue();
        Long projectId = ((Number) request.get("projectId")).longValue();
        String projectName = (String) request.get("projectName");

        log.info("Internal API: Sending invitation to user {} for project {}", userId, projectId);

        String token = internalUserService.createInvitationToken(userId, projectId, projectName);
        
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/validate-invitation-token")
    public ResponseEntity<BaseResponse<?>> validateInvitationToken(@RequestParam String token) {

        log.info("Validating invitation token");

        Optional<Map<String, Long>> result = internalUserService.validateInvitationToken(token);

        if (result.isPresent()) {
            Map<String, Long> data = result.get();
            return ResponseEntity.ok(new BaseResponse<>(1, "Token validated successfully", data));
        } else {
            return ResponseEntity.ok(new BaseResponse<>(0, "Invalid or expired token",
                    Map.of("error_code", "INVALID_TOKEN")));
        }
    }
}
