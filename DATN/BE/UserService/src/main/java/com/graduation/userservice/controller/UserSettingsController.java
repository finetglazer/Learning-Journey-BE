package com.graduation.userservice.controller;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.payload.request.UpdateSleepHoursRequest;
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

    @GetMapping("/sleep-hours")
    public ResponseEntity<?> getSleepHours(Principal principal) {
        try {
            Long userId = getUserIdFromPrincipal(principal);
            if (userId == null) {
                return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null));
            }

            BaseResponse<?> response = userSettingsService.getSleepHours(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_SLEEP_HOURS_FAILED, principal.getName(), e);
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_GET_SLEEP_HOURS_FAILED, null));
        }
    }

    @PutMapping("/sleep-hours")
    public ResponseEntity<?> updateSleepHours(
            @Valid @RequestBody UpdateSleepHoursRequest request,
            Principal principal) {
        try {
            Long userId = getUserIdFromPrincipal(principal);
            if (userId == null) {
                return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null));
            }

            BaseResponse<?> response = userSettingsService.updateSleepHours(userId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_UPDATE_SLEEP_HOURS_FAILED, principal.getName(), e);
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_UPDATE_SLEEP_HOURS_FAILED, null));
        }
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}