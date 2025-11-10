package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Deliverable;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.payload.request.CreateDeliverableRequest;
import com.graduation.projectservice.payload.request.UpdateDeliverableRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.repository.DeliverableRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.service.DeliverableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverableServiceImpl implements DeliverableService {

    private final DeliverableRepository deliverableRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationHelper authHelper;

    @Override
    @Transactional
    public BaseResponse<?> createDeliverable(Long userId, Long projectId, CreateDeliverableRequest request) {
        try {
            log.info(Constant.LOG_CREATING_DELIVERABLE, projectId, userId);

            // Authorization: Only OWNER can create deliverables
            authHelper.requireOwner(projectId, userId);

            // Get project and increment deliverable counter
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

            Long newCounter = project.getDeliverableCounter() + 1;
            project.setDeliverableCounter(newCounter);
            projectRepository.save(project);

            // Generate key with zero-padded counter (DEL-01, DEL-02, etc.)
            String key = String.format("DEL-%02d", newCounter);

            // Get the next order value
            Integer maxOrder = deliverableRepository.findMaxOrderByProjectId(projectId);
            Integer nextOrder = maxOrder + 1;

            // Create deliverable
            PM_Deliverable deliverable = new PM_Deliverable();
            deliverable.setProjectId(projectId);
            deliverable.setName(request.getName());
            deliverable.setKey(key);
            deliverable.setOrder(nextOrder);

            PM_Deliverable savedDeliverable = deliverableRepository.save(deliverable);

            log.info(Constant.LOG_DELIVERABLE_CREATED, savedDeliverable.getDeliverableId(), key, projectId);

            Map<String, Object> data = new HashMap<>();
            data.put("deliverableId", savedDeliverable.getDeliverableId());
            data.put("key", savedDeliverable.getKey());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_CREATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to create deliverable for project {} by user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateDeliverable(Long userId, Long projectId, Long deliverableId, UpdateDeliverableRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_DELIVERABLE, deliverableId, projectId, userId);

            // Authorization: Only OWNER can update deliverables
            authHelper.requireOwner(projectId, userId);

            PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_IN_PROJECT);
            }

            deliverable.setName(request.getName());
            deliverableRepository.save(deliverable);

            log.info(Constant.LOG_DELIVERABLE_UPDATED, deliverableId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_UPDATED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to update deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteDeliverable(Long userId, Long projectId, Long deliverableId) {
        try {
            log.info(Constant.LOG_DELETING_DELIVERABLE, deliverableId, projectId, userId);

            // Authorization: Only OWNER can delete deliverables
            authHelper.requireOwner(projectId, userId);

            PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_IN_PROJECT);
            }

            // Delete will cascade to phases and tasks (when those entities are created)
            deliverableRepository.delete(deliverable);

            log.info(Constant.LOG_DELIVERABLE_DELETED, deliverableId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.DELIVERABLE_DELETED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to delete deliverable {} for project {} by user {}", deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }
}