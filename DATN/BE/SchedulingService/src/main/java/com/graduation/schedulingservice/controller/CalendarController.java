package com.graduation.schedulingservice.controller;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.payload.response.CalendarListResponse;
import com.graduation.schedulingservice.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * Get user's calendar list
     * @param userId Extracted from X-User-Id header
     * @return Response containing the list of calendars
     */
    @GetMapping
    public ResponseEntity<BaseResponse<CalendarListResponse>> getUserCalendars(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            log.info(Constant.LOG_GET_USER_CALENDARS, userId);

            BaseResponse<CalendarListResponse> response = calendarService.getUserCalendars(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_GET_USER_CALENDARS_FAILED, userId, e);
            return ResponseEntity.ok(
                    new BaseResponse<>(0, Constant.MSG_CALENDARS_RETRIEVAL_FAILED, null)
            );
        }
    }
}