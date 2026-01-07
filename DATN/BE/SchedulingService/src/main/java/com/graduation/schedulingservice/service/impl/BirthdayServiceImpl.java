package com.graduation.schedulingservice.service.impl;

import com.graduation.schedulingservice.model.*;
import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.repository.CalendarItemRepository;
import com.graduation.schedulingservice.repository.CalendarRepository;
import com.graduation.schedulingservice.repository.MemorableEventRepository;
import com.graduation.schedulingservice.service.BirthdayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of BirthdayService.
 * Creates "My Birthday" memorable events following the pattern in
 * MemorableEventServiceImpl.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BirthdayServiceImpl implements BirthdayService {

    private static final String BIRTHDAY_TITLE = "My Birthday";
    private static final String BIRTHDAY_COLOR = "#FF6B9D"; // Pink color for memorable events
    private static final int YEARS_TO_GENERATE = 5;

    private final MemorableEventRepository memorableEventRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final CalendarRepository calendarRepository;

    @Override
    @Transactional
    public void createOrUpdateBirthday(Long userId, int day, int month) {
        try {
            log.info("Creating/updating birthday for userId={}, day={}, month={}", userId, day, month);

            // Step 1: Get user's private calendar
            Calendar privateCalendar = calendarRepository.findByUserId(userId).stream()
                    .filter(cal -> cal.getType().toString().equals("PERSONAL"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Private calendar not found for user " + userId));

            // Step 2: Find existing "My Birthday" memorable event for this user
            List<MemorableEvent> existingEvents = memorableEventRepository.findByUserId(userId);
            Optional<MemorableEvent> existingBirthday = existingEvents.stream()
                    .filter(event -> BIRTHDAY_TITLE.equals(event.getTitle()))
                    .findFirst();

            // Step 3: Delete existing birthday calendar items if exists
            if (existingBirthday.isPresent()) {
                Long existingEventId = existingBirthday.get().getId();
                calendarItemRepository.deleteByMemorableEventIdIn(List.of(existingEventId));
                memorableEventRepository.delete(existingBirthday.get());
                log.info("Deleted existing birthday event and calendar items for userId={}", userId);
            }

            // Step 4: Create new MemorableEvent for birthday
            MemorableEvent birthdayEvent = new MemorableEvent();
            birthdayEvent.setUserId(userId);
            birthdayEvent.setTitle(BIRTHDAY_TITLE);
            birthdayEvent.setDay(day);
            birthdayEvent.setMonth(month);

            MemorableEvent savedEvent = memorableEventRepository.save(birthdayEvent);
            log.info("Created birthday memorable event: id={}, userId={}", savedEvent.getId(), userId);

            // Step 5: Generate calendar items for next 5 years
            int calendarItemsGenerated = 0;
            int currentYear = LocalDate.now().getYear();

            for (int yearOffset = 0; yearOffset < YEARS_TO_GENERATE; yearOffset++) {
                int targetYear = currentYear + yearOffset;

                // Create MemorableEventCalendarItem
                MemorableEventCalendarItem calendarEvent = new MemorableEventCalendarItem();
                calendarEvent.setUserId(userId);
                calendarEvent.setCalendarId(privateCalendar.getId());
                calendarEvent.setName(BIRTHDAY_TITLE);
                calendarEvent.setMemorableEventId(savedEvent.getId());
                calendarEvent.setStatus(ItemStatus.INCOMPLETE);
                calendarEvent.setColor(BIRTHDAY_COLOR);

                // Create TimeSlot for the event (all-day event)
                LocalDateTime startDateTime = LocalDateTime.of(
                        targetYear,
                        month,
                        day,
                        0, 0);
                LocalDateTime endDateTime = startDateTime.plusDays(1);

                TimeSlot timeSlot = new TimeSlot();
                timeSlot.setStartTime(startDateTime);
                timeSlot.setEndTime(endDateTime);

                calendarEvent.setTimeSlot(timeSlot);

                calendarItemRepository.save(calendarEvent);
                calendarItemsGenerated++;
            }

            log.info("Generated {} birthday calendar items for userId={} over {} years",
                    calendarItemsGenerated, userId, YEARS_TO_GENERATE);

        } catch (Exception e) {
            log.error("Failed to create/update birthday for userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create birthday memorable event", e);
        }
    }
}
