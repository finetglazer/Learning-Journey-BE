package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.payload.request.MemorableEventDTO;
import com.graduation.schedulingservice.payload.request.UpdateMemorableEventsRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;
import com.graduation.schedulingservice.payload.response.MemorableEventResponseDTO;

import com.graduation.schedulingservice.payload.response.MemorableEventsResponse;
import com.graduation.schedulingservice.payload.response.UpdateMemorableEventsResponse;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.repository.MemorableEventRepository;
import com.graduation.schedulingservice.service.MemorableEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemorableEventServiceImpl implements MemorableEventService {

    private final MemorableEventRepository memorableEventRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final CalendarRepository calendarRepository;

    @Override
    public BaseResponse<?> getMemorableEvents(Long userId) {
        try {
            log.info("Retrieving memorable events for userId={}", userId);

            List<MemorableEvent> events = memorableEventRepository.findByUserId(userId);

            List<MemorableEventResponseDTO> eventDTOs = events.stream()
                    .map(event -> MemorableEventResponseDTO.builder()
                            .id(event.getId())
                            .title(event.getTitle())
                            .day(event.getDay())
                            .month(event.getMonth())
                            .build())
                    .collect(Collectors.toList());

            MemorableEventsResponse response = MemorableEventsResponse.builder()
                    .events(eventDTOs)
                    .build();

            log.info("Successfully retrieved {} memorable events for userId={}", eventDTOs.size(), userId);
            return new BaseResponse<>(1, "Memorable events retrieved", response);

        } catch (Exception e) {
            log.error("Failed to retrieve memorable events for userId={}", userId, e);
            return new BaseResponse<>(0, "Failed to retrieve memorable events", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateMemorableEvents(Long userId, UpdateMemorableEventsRequest request) {
        try {
            log.info("Updating memorable events for userId={}, newEventCount={}",
                    userId, request.getEvents().size());

            // Step 1: Get user's private calendar
            Calendar privateCalendar = calendarRepository.findByUserId(userId).stream()
                    .filter(cal -> cal.getType().toString().equals("PERSONAL"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Private calendar not found for user"));

            // Step 2: Delete all old memorable events and their linked calendar items
            List<MemorableEvent> oldEvents = memorableEventRepository.findByUserId(userId);
            List<Long> oldEventIds = oldEvents.stream()
                    .map(MemorableEvent::getId)
                    .collect(Collectors.toList());

            // Delete calendar items linked to old memorable events
            if (!oldEventIds.isEmpty()) {
                calendarItemRepository.deleteByMemorableEventIdIn(oldEventIds);
                log.info("Deleted calendar items linked to {} memorable events", oldEventIds.size());
            }

            // Delete old memorable events
            memorableEventRepository.deleteByUserId(userId);
            log.info("Deleted {} old memorable events for userId={}", oldEvents.size(), userId);

            // Step 3: Create new memorable events
            List<MemorableEvent> newEvents = new ArrayList<>();
            for (MemorableEventDTO dto : request.getEvents()) {
                MemorableEvent event = new MemorableEvent();
                event.setUserId(userId);
                event.setTitle(dto.getTitle());
                event.setDay(dto.getDay());
                event.setMonth(dto.getMonth());
                newEvents.add(event);
            }

            List<MemorableEvent> savedEvents = memorableEventRepository.saveAll(newEvents);
            log.info("Created {} new memorable events for userId={}", savedEvents.size(), userId);

            // Step 4: Generate calendar Event items for next 5 years
            int calendarItemsGenerated = 0;
            int currentYear = LocalDate.now().getYear();

            for (MemorableEvent memorableEvent : savedEvents) {
                for (int yearOffset = 0; yearOffset < 5; yearOffset++) {
                    int targetYear = currentYear + yearOffset;

                    // CREATE MemorableEventCalendarItem instead of Event
                    MemorableEventCalendarItem calendarEvent = new MemorableEventCalendarItem();
                    calendarEvent.setUserId(userId);
                    calendarEvent.setCalendarId(privateCalendar.getId());
                    calendarEvent.setName(memorableEvent.getTitle());
                    calendarEvent.setMemorableEventId(memorableEvent.getId());
                    calendarEvent.setStatus(ItemStatus.INCOMPLETE);
                    calendarEvent.setColor("#FF6B9D"); // Pink color for memorable events

                    // Create TimeSlot for the event (all-day event)
                    LocalDateTime startDateTime = LocalDateTime.of(
                            targetYear,
                            memorableEvent.getMonth(),
                            memorableEvent.getDay(),
                            0, 0
                    );
                    LocalDateTime endDateTime = startDateTime.plusDays(1);

                    TimeSlot timeSlot = new TimeSlot();
                    timeSlot.setStartTime(startDateTime);
                    timeSlot.setEndTime(endDateTime);
//                    timeSlot.setTimezone("UTC+07:00"); // Default timezone, should be from user settings

                    calendarEvent.setTimeSlot(timeSlot);

                    calendarItemRepository.save(calendarEvent);
                    calendarItemsGenerated++;
                }
            }

            log.info("Generated {} calendar items for {} memorable events over 5 years",
                    calendarItemsGenerated, savedEvents.size());

            UpdateMemorableEventsResponse response = UpdateMemorableEventsResponse.builder()
                    .eventsCreated(savedEvents.size())
                    .calendarItemsGenerated(calendarItemsGenerated)
                    .build();

            return new BaseResponse<>(1, "Memorable events updated, calendar events generated", response);

        } catch (Exception e) {
            log.error("Failed to update memorable events for userId={}", userId, e);
            return new BaseResponse<>(0, "Failed to update memorable events", null);
        }
    }
}