package com.graduation.userservice.controller;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.payload.request.*;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        BaseResponse<?> response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.MSG_TOKEN_REQUIRED, null));
        }
        BaseResponse<?> response = authService.verifyUser(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        BaseResponse<?> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(Constant.HEADER_AUTHORIZATION) String authHeader) {
        if (authHeader != null && authHeader.startsWith(Constant.PREFIX_BEARER)) {
            String token = authHeader.substring(7);
            BaseResponse<?> response = authService.logout(token);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(new BaseResponse<>(1, Constant.MSG_LOGOUT_SUCCESS, null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal,
            @RequestHeader(Constant.HEADER_AUTHORIZATION) String authHeader) {

        String token = "";
        if (authHeader != null && authHeader.startsWith(Constant.PREFIX_BEARER)) {
            token = authHeader.substring(7);
        }

        BaseResponse<?> response = authService.changePassword(request, principal, token);

        // **FIXED**: Always return 200 OK and let the JSON body indicate success or failure.
        // This makes it consistent with the register endpoint.
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        BaseResponse<?> response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        BaseResponse<?> response = authService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(response);
    }


}