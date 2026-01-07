package com.graduation.userservice.service.impl;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.event.UserProfileEventPublisher;
import com.graduation.userservice.model.User;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.payload.response.ProfileResponse;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.GcsStorageService;
import com.graduation.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final GcsStorageService gcsStorageService;
    private final UserProfileEventPublisher userProfileEventPublisher;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getProfile(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            ProfileResponse response = new ProfileResponse(
                    user.getDisplayName(),
                    String.valueOf(user.getDateOfBirth()),
                    user.getAvatarUrl(),
                    user.getEmail());

            log.info(Constant.LOG_GET_PROFILE_SUCCESS, userId);
            return new BaseResponse<>(1, Constant.MSG_GET_PROFILE_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_PROFILE_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_GET_PROFILE_FAILED, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateProfile(Long userId, String name, String dateOfBirth, MultipartFile avatar) {
        try {
            // 1. Verify user exists
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            // 2. Validate and update name
            if (name == null || name.trim().isEmpty()) {
                log.warn(Constant.LOG_INVALID_NAME, userId);
                return new BaseResponse<>(0, Constant.MSG_NAME_REQUIRED, null);
            }

            if (name.trim().length() > 100) {
                log.warn(Constant.LOG_NAME_TOO_LONG, userId);
                return new BaseResponse<>(0, Constant.MSG_NAME_TOO_LONG, null);
            }

            user.setDisplayName(name.trim());

            // 3. Validate and update date of birth
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    // Try to parse the date
                    LocalDate parsedDate = LocalDate.parse(dateOfBirth, DATE_FORMATTER);
                    user.setDateOfBirth(parsedDate);

                } catch (DateTimeParseException e) {
                    // If parsing fails, the format is invalid.
                    // (Assuming you add these new constants)
                    log.warn(Constant.LOG_INVALID_DOB_FORMAT, userId, dateOfBirth);
                    return new BaseResponse<>(0, Constant.MSG_INVALID_DOB_FORMAT, null);
                }
            }

            // 4. Handle avatar upload if provided
            String newAvatarUrl = null;
            if (avatar != null && !avatar.isEmpty()) {
                try {
                    // Delete old avatar if exists
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        gcsStorageService.deleteAvatar(userId);
                    }

                    // Upload new avatar
                    newAvatarUrl = gcsStorageService.uploadAvatar(userId, avatar);
                    user.setAvatarUrl(newAvatarUrl);

                    // --- NEW CATCH BLOCK ---
                    // Specifically catch the "file too large" error first
                } catch (MaxUploadSizeExceededException e) {
                    log.warn(Constant.LOG_AVATAR_FILE_TOO_LARGE, userId, avatar.getSize());
                    return new BaseResponse<>(0, Constant.MSG_FILE_TOO_LARGE, null);
                    // --- END OF NEW CATCH BLOCK ---

                } catch (IllegalArgumentException e) {
                    // Catches other validation errors (file type, file required)
                    log.warn(Constant.LOG_AVATAR_VALIDATION_FAILED, userId, e.getMessage());
                    return new BaseResponse<>(0, e.getMessage(), null);
                } catch (Exception e) {
                    // Upload error
                    log.error(Constant.LOG_AVATAR_UPLOAD_FAILED, userId, e);
                    return new BaseResponse<>(0, Constant.MSG_AVATAR_UPLOAD_FAILED, null);
                }
            }
            // 5. Save user
            userRepository.save(user);

            // 6. Publish event to Kafka for cache sync (async)
            userProfileEventPublisher.publishUserUpdatedEvent(user);

            // 6.1. Publish birthday event if date of birth was updated
            if (user.getDateOfBirth() != null) {
                userProfileEventPublisher.publishBirthdayUpdatedEvent(userId, user.getDateOfBirth());
            }

            // 7. Build response
            ProfileResponse response = new ProfileResponse(
                    user.getDisplayName(),
                    dateOfBirth,
                    user.getAvatarUrl(),
                    user.getEmail());

            log.info(Constant.LOG_UPDATE_PROFILE_SUCCESS, userId);
            return new BaseResponse<>(1, Constant.MSG_UPDATE_PROFILE_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_UPDATE_PROFILE_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_UPDATE_PROFILE_FAILED, null);
        }
    }
}
