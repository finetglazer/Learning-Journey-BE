package com.graduation.userservice.service;

import com.graduation.userservice.payload.response.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {
    BaseResponse<?> getProfile(Long userId);
    
    BaseResponse<?> updateProfile(Long userId, String name, String dateOfBirth, MultipartFile avatar);
}
