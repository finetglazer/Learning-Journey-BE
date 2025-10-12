package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.Calendar;
import com.graduation.schedulingservice.model.enums.CalendarType;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;

    @Override
    @Transactional
    public BaseResponse<?> createDefaultCalendar(Long userId) {
        try {
            // Check if user already has a calendar
            if (!calendarRepository.findByUserId(userId).isEmpty()) {
                log.warn("User {} already has calendars, skipping default creation", userId);
                return new BaseResponse<>(1, "User already has calendars", null);
            }

            // Create default calendar
            Calendar defaultCalendar = new Calendar();
            defaultCalendar.setUserId(userId);
            defaultCalendar.setName("My Calendar");
            defaultCalendar.setDescription("Default personal calendar");
            defaultCalendar.setType(CalendarType.PERSONAL);
            defaultCalendar.setIsVisible(true);
            defaultCalendar.setIsPinned(true);

            Calendar saved = calendarRepository.save(defaultCalendar);

            log.info("Default calendar created for user {}: calendarId={}", userId, saved.getId());
            return new BaseResponse<>(1, "Default calendar created successfully", saved.getId());

        } catch (Exception e) {
            log.error("Failed to create default calendar for user {}", userId, e);
            return new BaseResponse<>(0, "Failed to create default calendar", null);
        }
    }
}