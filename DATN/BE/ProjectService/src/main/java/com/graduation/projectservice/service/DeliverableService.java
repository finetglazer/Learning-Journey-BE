package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateDeliverableRequest;
import com.graduation.projectservice.payload.request.UpdateDeliverableRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface DeliverableService {

    BaseResponse<?> createDeliverable(Long userId, Long projectId, CreateDeliverableRequest request);

    BaseResponse<?> updateDeliverable(Long userId, Long projectId, Long deliverableId,
            UpdateDeliverableRequest request);

    BaseResponse<?> deleteDeliverable(Long userId, Long projectId, Long deliverableId);

    /**
     * Get project structure with all deliverables and their phases (no tasks)
     */
    BaseResponse<?> getProjectStructure(Long projectId, Long userId, String search);

    /**
     * Get lightweight project skeleton with deliverables and phases only.
     * Tasks are NOT loaded - only task counts per phase.
     * For initial structure loading before lazy-loading tasks.
     */
    BaseResponse<?> getProjectSkeleton(Long projectId, Long userId);
}