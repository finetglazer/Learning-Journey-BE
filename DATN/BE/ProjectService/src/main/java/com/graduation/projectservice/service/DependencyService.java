package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.DependencyRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface DependencyService {

    /**
     * Lazy load dependencies for a specific item (Task/Phase/Deliverable)
     */
    BaseResponse<?> getDependencies(Long userId, Long projectId, Long itemId, String itemType);

    /**
     * Create a link between two items
     */
    BaseResponse<?> createDependency(Long userId, Long projectId, DependencyRequest request);

    /**
     * Remove a link between two items
     */
    BaseResponse<?> deleteDependency(Long userId, Long projectId, DependencyRequest request);
}