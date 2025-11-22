package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateDeliverableRequest;
import com.graduation.projectservice.payload.request.UpdateDeliverableRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface DeliverableService {

    BaseResponse<?> createDeliverable(Long userId, Long projectId, CreateDeliverableRequest request);

    BaseResponse<?> updateDeliverable(Long userId, Long projectId, Long deliverableId, UpdateDeliverableRequest request);

    BaseResponse<?> deleteDeliverable(Long userId, Long projectId, Long deliverableId);

    /**
     * Get project structure with all deliverables and their phases (no tasks)
     */
    BaseResponse<?> getProjectStructure(Long projectId, Long userId, String search);
}