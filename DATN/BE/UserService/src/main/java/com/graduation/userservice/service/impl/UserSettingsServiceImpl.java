package com.graduation.userservice.service.impl;

import com.graduation.userservice.client.SchedulingServiceClient;
import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.model.TimeRange;
import com.graduation.userservice.model.User;
import com.graduation.userservice.payload.request.UpdateTimezoneRequest;
import com.graduation.userservice.payload.response.*;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.UserSettingsService;
import com.graduation.userservice.utils.TimezoneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {


    private final UserRepository userRepository;
    private final SchedulingServiceClient schedulingServiceClient;
    private final TimezoneMapper timezoneMapper; // ADD THIS

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> VALID_ITEM_TYPES = List.of("TASK", "ROUTINE");

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getTimezone(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    // The DB stores the IANA ID, e.g., "America/New_York"
                    String ianaId = user.getPreferredTimezone();

                    // ✅ Dynamically convert it to the display format for the client
                    String displayTimezone = timezoneMapper.toDisplayFormat(ianaId);

                    log.info("Successfully retrieved timezone '{}' for user {}", displayTimezone, userId);
                    Map<String, String> data = Map.of("timezone", displayTimezone);
                    return new BaseResponse<>(1, "Timezone retrieved successfully", data);
                })
                .orElseGet(() -> {
                    log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                    return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
                });
    }

    @Override
    @Transactional
    public BaseResponse<?> updateTimezone(Long userId, UpdateTimezoneRequest request) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            // The request should contain the IANA ID, e.g., "America/New_York"
            String newIanaId = request.getTimezone();
            String oldIanaId = user.getPreferredTimezone();

            // 1. Validate the new IANA ID
            if (!timezoneMapper.isValidIanaId(newIanaId)) {
                log.warn("Invalid or unsupported timezone IANA ID provided by user {}: {}", userId, newIanaId);
                return new BaseResponse<>(0, "Invalid or unsupported timezone provided.", null);
            }

            // 2. Check if timezone actually changed
            if (oldIanaId.equals(newIanaId)) {
                log.info("Timezone unchanged for user {}: {}", userId, newIanaId);
                return new BaseResponse<>(1, "Timezone is already set.", null);
            }

            // 3. ✅ Update the user's timezone field (store the IANA ID)
            user.setPreferredTimezone(newIanaId);
            userRepository.save(user);

            // 4. Call SchedulingService with the IANA IDs (this part was already correct)
            boolean conversionSuccess = schedulingServiceClient.convertUserTimezone(
                    userId, oldIanaId, newIanaId
            );

            if (!conversionSuccess) {
                log.warn("Calendar items conversion failed for user {}, but user timezone is updated", userId);
                return new BaseResponse<>(1, "Timezone updated successfully. Calendar items will be synchronized shortly.", null);
            }

            String newDisplayTimezone = timezoneMapper.toDisplayFormat(newIanaId);
            log.info("Successfully updated timezone for user {} from {} to {}", userId, oldIanaId, newIanaId);
            return new BaseResponse<>(1, "Timezone updated to " + newDisplayTimezone, null);

        } catch (Exception e) {
            log.error("Failed to update timezone for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to update timezone", null);
        }
    }
}