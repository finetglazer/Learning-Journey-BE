package com.graduation.userservice.service.impl;

import com.graduation.userservice.client.SchedulingServiceClient;
import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.model.TimeRange;
import com.graduation.userservice.model.User;
import com.graduation.userservice.model.UserConstraints;
import com.graduation.userservice.payload.request.UpdateDailyLimitsRequest;
import com.graduation.userservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.userservice.payload.request.UpdateTimezoneRequest;
import com.graduation.userservice.payload.response.*;
import com.graduation.userservice.repository.UserConstraintsRepository;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
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

    private final UserConstraintsRepository userConstraintsRepository;
    private final UserRepository userRepository;
    private final SchedulingServiceClient schedulingServiceClient; // ADD THIS

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> VALID_ITEM_TYPES = List.of("TASK", "ROUTINE");
    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getSleepHours(Long userId) {
        try {
            // Verify user exists
            if (!userRepository.existsById(userId)) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            // Get or create default constraints
            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            // Convert to DTO
            SleepHoursResponse response = new SleepHoursResponse();
            response.setSleepHours(
                    constraints.getSleepHours().stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList())
            );

            log.info(Constant.LOG_GET_SLEEP_HOURS_SUCCESS, userId);
            return new BaseResponse<>(1, Constant.MSG_GET_SLEEP_HOURS_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_SLEEP_HOURS_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_GET_SLEEP_HOURS_FAILED, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSleepHours(Long userId, UpdateSleepHoursRequest request) {
        try {
            // Verify user exists
            if (!userRepository.existsById(userId)) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            // Validate time ranges
            List<TimeRange> timeRanges = new ArrayList<>();
            for (TimeRangeDto dto : request.getSleepHours()) {
                try {
                    LocalTime start = LocalTime.parse(dto.getStartTime(), TIME_FORMATTER);
                    LocalTime end = LocalTime.parse(dto.getEndTime(), TIME_FORMATTER);

                    // Validate start != end
                    if (start.equals(end)) {
                        return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_RANGE_SAME, null);
                    }

                    timeRanges.add(new TimeRange(start, end));
                } catch (DateTimeParseException e) {
                    log.warn(Constant.LOG_INVALID_TIME_FORMAT, dto.getStartTime(), dto.getEndTime());
                    return new BaseResponse<>(0, Constant.MSG_INVALID_TIME_FORMAT, null);
                }
            }

            // Get or create constraints
            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            // Update sleep hours
            constraints.updateSleepHours(timeRanges);
            userConstraintsRepository.save(constraints);

            log.info(Constant.LOG_UPDATE_SLEEP_HOURS_SUCCESS, userId, timeRanges.size());
            return new BaseResponse<>(1, Constant.MSG_UPDATE_SLEEP_HOURS_SUCCESS, null);

        } catch (Exception e) {
            log.error(Constant.LOG_UPDATE_SLEEP_HOURS_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_UPDATE_SLEEP_HOURS_FAILED, null);
        }
    }

    private TimeRangeDto convertToDto(TimeRange timeRange) {
        return new TimeRangeDto(
                timeRange.getStartTime().format(TIME_FORMATTER),
                timeRange.getEndTime().format(TIME_FORMATTER)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getDailyLimits(Long userId) {
        try {
            if (!userRepository.existsById(userId)) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            // Convert to DTO
            Map<String, DailyLimitDto> limitsMap = new HashMap<>();
            for (String itemType : VALID_ITEM_TYPES) {
                Integer hours = constraints.getDailyLimits().getOrDefault(itemType, 8); // Default 8 hours
                // REMOVED: 'enabled' is now global
                limitsMap.put(itemType, new DailyLimitDto(hours)); // CHANGED
            }

            // CHANGED: Create response with the global enabled flag
            DailyLimitsResponse response = new DailyLimitsResponse(constraints.getDailyLimitFeatureEnabled(), limitsMap);

            log.info(Constant.LOG_GET_DAILY_LIMITS_SUCCESS, userId);
            return new BaseResponse<>(1, Constant.MSG_GET_DAILY_LIMITS_SUCCESS, response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_DAILY_LIMITS_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_GET_DAILY_LIMITS_FAILED, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDailyLimits(Long userId, UpdateDailyLimitsRequest request) {
        try {
            if (!userRepository.existsById(userId)) {
                log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            // Validate item types
            for (String itemType : request.getLimits().keySet()) {
                if (!VALID_ITEM_TYPES.contains(itemType)) {
                    log.warn(Constant.LOG_INVALID_ITEM_TYPE, itemType);
                    return new BaseResponse<>(0, Constant.MSG_INVALID_ITEM_TYPE + itemType, null);
                }
            }

            // Validate hours (must be between 0 and 24)
            for (Map.Entry<String, DailyLimitDto> entry : request.getLimits().entrySet()) {
                Integer hours = entry.getValue().getHours();
                if (hours < 0 || hours > 24) {
                    log.warn(Constant.LOG_INVALID_HOURS_VALUE, entry.getKey(), hours);
                    return new BaseResponse<>(0, Constant.MSG_INVALID_HOURS_RANGE, null);
                }
            }

            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            // ADDED: Update the global feature enabled status
            constraints.setDailyLimitFeatureEnabled(request.getEnabled());

            // Update daily limits (hours only)
            for (Map.Entry<String, DailyLimitDto> entry : request.getLimits().entrySet()) {
                String itemType = entry.getKey();
                DailyLimitDto limit = entry.getValue();
                // CHANGED: Call updated method in UserConstraints
                constraints.updateDailyLimit(itemType, limit.getHours());
            }

            userConstraintsRepository.save(constraints);

            log.info(Constant.LOG_UPDATE_DAILY_LIMITS_SUCCESS, userId, request.getLimits().size());
            return new BaseResponse<>(1, Constant.MSG_UPDATE_DAILY_LIMITS_SUCCESS, null);

        } catch (Exception e) {
            log.error(Constant.LOG_UPDATE_DAILY_LIMITS_FAILED, userId, e);
            return new BaseResponse<>(0, Constant.MSG_UPDATE_DAILY_LIMITS_FAILED, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getTimezone(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    log.info("Successfully retrieved timezone '{}' for user {}", user.getPreferredTimezone(), userId);
                    // Using a simple Map for the response body to match your API design
                    return new BaseResponse<>(1, "Timezone retrieved successfully", Map.of("timezone", user.getPreferredTimezone()));
                })
                .orElseGet(() -> {
                    log.warn(Constant.LOG_USER_NOT_FOUND, userId);
                    return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
                });
    }

    @Override
    @Transactional
    public BaseResponse<?> updateTimezone(Long userId, UpdateTimezoneRequest request) {
        // 1. Find the User
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn(Constant.LOG_USER_NOT_FOUND, userId);
            return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
        }

        String newTimezoneStr = request.getTimezone();
        String oldTimezoneStr = user.getPreferredTimezone();

        // 2. Validate the timezone string
        try {
            ZoneId.of(newTimezoneStr);
        } catch (DateTimeException e) {
            log.warn("Invalid timezone format provided by user {}: {}", userId, newTimezoneStr);
            return new BaseResponse<>(0, "Invalid timezone format provided.", null);
        }

        // 3. Check if timezone actually changed
        if (oldTimezoneStr.equals(newTimezoneStr)) {
            log.info("Timezone unchanged for user {}: {}", userId, newTimezoneStr);
            return new BaseResponse<>(1, "Timezone is already set to " + newTimezoneStr, null);
        }

        // 4. Update the user's timezone field
        user.setPreferredTimezone(newTimezoneStr);
        userRepository.save(user);

        // 5. Call SchedulingService to convert all calendar items
        boolean conversionSuccess = schedulingServiceClient.convertUserTimezone(
                userId, oldTimezoneStr, newTimezoneStr
        );

        if (!conversionSuccess) {
            log.warn("Calendar items conversion failed for user {}, but user timezone is updated", userId);
            // Still return success since user timezone is updated
            // Calendar items conversion failure is logged and can be retried
            return new BaseResponse<>(1,
                    "Timezone updated successfully. Calendar items will be synchronized shortly.",
                    null);
        }

        log.info("Successfully updated timezone for user {} from {} to {}",
                userId, oldTimezoneStr, newTimezoneStr);
        return new BaseResponse<>(1, "Timezone updated successfully.", null);
    }
}