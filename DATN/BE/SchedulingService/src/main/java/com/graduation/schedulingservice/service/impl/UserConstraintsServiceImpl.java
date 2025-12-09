package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.TimeRange;
import com.graduation.schedulingservice.model.UserConstraints;
import com.graduation.schedulingservice.payload.request.UpdateDailyLimitsRequest;
import com.graduation.schedulingservice.payload.request.UpdateSleepHoursRequest;
import com.graduation.schedulingservice.payload.response.*;
import com.graduation.schedulingservice.repository.UserConstraintsRepository;
import com.graduation.schedulingservice.service.UserConstraintsService;
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
public class UserConstraintsServiceImpl implements UserConstraintsService {

    private final UserConstraintsRepository userConstraintsRepository;

    // TODO: Define these constants in your SchedulingService Constant file
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> VALID_ITEM_TYPES = List.of("TASK", "ROUTINE");

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getSleepHours(Long userId) {
        try {
            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            SleepHoursResponse response = new SleepHoursResponse();
            response.setSleepHours(
                    constraints.getSleepHours().stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList())
            );
            return new BaseResponse<>(1, "Sleep hours retrieved successfully", response);
        } catch (Exception e) {
            log.error("Failed to get sleep hours for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to get sleep hours", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSleepHours(Long userId, UpdateSleepHoursRequest request) {
        try {
            List<TimeRange> timeRanges = new ArrayList<>();
            for (TimeRangeDto dto : request.getSleepHours()) {
                try {
                    LocalTime start = LocalTime.parse(dto.getStartTime(), TIME_FORMATTER);
                    LocalTime end = LocalTime.parse(dto.getEndTime(), TIME_FORMATTER);

                    if (start.equals(end)) {
                        return new BaseResponse<>(0, "Start time cannot equal end time", null);
                    }
                    timeRanges.add(new TimeRange(start, end));
                } catch (DateTimeParseException e) {
                    return new BaseResponse<>(0, "Invalid time format (HH:mm required)", null);
                }
            }

            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            constraints.updateSleepHours(timeRanges);
            userConstraintsRepository.save(constraints);

            return new BaseResponse<>(1, "Sleep hours updated successfully", null);
        } catch (Exception e) {
            log.error("Failed to update sleep hours for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to update sleep hours", null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getDailyLimits(Long userId) {
        try {
            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            Map<String, DailyLimitDto> limitsMap = new HashMap<>();
            for (String itemType : VALID_ITEM_TYPES) {
                Integer hours = constraints.getDailyLimits().getOrDefault(itemType, 8);
                limitsMap.put(itemType, new DailyLimitDto(hours));
            }

            DailyLimitsResponse response = new DailyLimitsResponse(
                    constraints.getDailyLimitFeatureEnabled(),
                    limitsMap
            );
            return new BaseResponse<>(1, "Daily limits retrieved successfully", response);
        } catch (Exception e) {
            log.error("Failed to get daily limits for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to get daily limits", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDailyLimits(Long userId, UpdateDailyLimitsRequest request) {
        try {
            // Validate Item Types
            for (String itemType : request.getLimits().keySet()) {
                if (!VALID_ITEM_TYPES.contains(itemType)) {
                    return new BaseResponse<>(0, "Invalid item type: " + itemType, null);
                }
            }

            UserConstraints constraints = userConstraintsRepository.findByUserId(userId)
                    .orElseGet(() -> UserConstraints.createDefault(userId));

            constraints.setDailyLimitFeatureEnabled(request.getEnabled());

            for (Map.Entry<String, DailyLimitDto> entry : request.getLimits().entrySet()) {
                constraints.updateDailyLimit(entry.getKey(), entry.getValue().getHours());
            }

            userConstraintsRepository.save(constraints);
            return new BaseResponse<>(1, "Daily limits updated successfully", null);
        } catch (Exception e) {
            log.error("Failed to update daily limits for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to update daily limits", null);
        }
    }

    private TimeRangeDto convertToDto(TimeRange timeRange) {
        return new TimeRangeDto(
                timeRange.getStartTime().format(TIME_FORMATTER),
                timeRange.getEndTime().format(TIME_FORMATTER)
        );
    }
}