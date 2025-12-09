package com.graduation.userservice.controller;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.payload.request.UpdateTimezoneRequest;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/users/constraints")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final UserRepository userRepository;

    private Long getUserIdFromPrincipal(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .map(user -> user.getId())
                .orElse(null);
    }

    @GetMapping("/timezone")
    public ResponseEntity<?> getTimezone(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null));
        }
        BaseResponse<?> response = userSettingsService.getTimezone(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/timezone")
    public ResponseEntity<?> updateTimezone(
            @Valid @RequestBody UpdateTimezoneRequest request,
            Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null));
        }
        BaseResponse<?> response = userSettingsService.updateTimezone(userId, request);
        return ResponseEntity.ok(response);
    }
}