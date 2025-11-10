package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.payload.request.UpdateMemorableEventsRequest;
import com.graduation.schedulingservice.payload.response.BaseResponse;

public interface MemorableEventService {

    /**
     * Retrieve all memorable events configured by the user
     *
     * @param userId The authenticated user ID
     * @return BaseResponse containing all memorable events
     */
    BaseResponse<?> getMemorableEvents(Long userId);

    /**
     * Replace entire list of memorable events
     * Deletes old events and calendar items, creates new ones
     * Auto-generates calendar Event items for next 5 years
     *
     * @param userId The authenticated user ID
     * @param request The request containing the new list of memorable events
     * @return BaseResponse containing the number of events created and calendar items generated
     */
    BaseResponse<?> updateMemorableEvents(Long userId, UpdateMemorableEventsRequest request);
}