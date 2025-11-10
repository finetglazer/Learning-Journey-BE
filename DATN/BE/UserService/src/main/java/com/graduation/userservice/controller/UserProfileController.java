package com.graduation.userservice.controller;

import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.security.JwtProvider;
import com.graduation.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtProvider jwtProvider;

    /**
     * GET /api/user/profile
     * Retrieve current user's profile
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getProfile(
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        Long userId = Long.parseLong(userIdHeader);
        BaseResponse<?> response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/user/profile
     * Update current user's profile (multipart/form-data)
     * 
     * Form fields:
     * - name: String (required)
     * - dateOfBirth: String in dd/MM/yyyy format (optional)
     * - avatar: MultipartFile (optional)
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<?>> updateProfile(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestPart(value = "name", required = true) String name,
            @RequestPart(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        Long userId = Long.parseLong(userIdHeader);
        BaseResponse<?> response = userProfileService.updateProfile(userId, name, dateOfBirth, avatar);
        return ResponseEntity.ok(response);
    }
}
