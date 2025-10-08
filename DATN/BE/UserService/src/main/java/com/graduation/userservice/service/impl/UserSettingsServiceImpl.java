package com.graduation.userservice.service.impl;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.model.TimeRange;
import com.graduation.userservice.model.UserConstraints;
import com.graduation.userservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.payload.response.SleepHoursResponse;
import com.graduation.userservice.payload.response.TimeRangeDto;
import com.graduation.userservice.repository.UserConstraintsRepository;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserConstraintsRepository userConstraintsRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
}