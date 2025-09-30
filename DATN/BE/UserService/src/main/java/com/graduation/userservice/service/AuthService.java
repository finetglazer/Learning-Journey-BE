package com.graduation.userservice.service;

import com.graduation.userservice.payload.request.ChangePasswordRequest;
import com.graduation.userservice.payload.request.LoginRequest;
import com.graduation.userservice.payload.request.RegisterRequest;
import com.graduation.userservice.payload.response.BaseResponse;

import java.security.Principal;

public interface AuthService {
    BaseResponse<?> register(RegisterRequest request);
    BaseResponse<?> verifyUser(String token);
    BaseResponse<?> login(LoginRequest request);
    BaseResponse<?> logout(String token);
    BaseResponse<?> changePassword(ChangePasswordRequest request, Principal principal, String currentSessionToken);

    BaseResponse<?> forgotPassword(String email);
    BaseResponse<?> resetPassword(String token, String newPassword, String confirmPassword);
}