package com.graduation.schedulingservice.service;

/**
 * Service for managing birthday memorable events.
 * Creates/updates "My Birthday" events when users set their date of birth.
 */
public interface BirthdayService {

    /**
     * Create or update a user's birthday as a memorable event.
     * This will:
     * 1. Delete any existing "My Birthday" memorable event for the user
     * 2. Create a new MemorableEvent with the given day/month
     * 3. Generate MemorableEventCalendarItem entries for the next 5 years
     *
     * @param userId The user ID
     * @param day    Day of the month (1-31)
     * @param month  Month of the year (1-12)
     */
    void createOrUpdateBirthday(Long userId, int day, int month);
}
