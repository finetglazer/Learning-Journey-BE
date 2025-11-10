package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Deliverable;
import com.graduation.projectservice.model.PM_Phase;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.payload.request.CreatePhaseRequest;
import com.graduation.projectservice.payload.request.UpdatePhaseRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.repository.DeliverableRepository;
import com.graduation.projectservice.repository.PhaseRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.service.PhaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {

    private final PhaseRepository phaseRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationHelper authHelper;

    @Override
    @Transactional
    public BaseResponse<?> createPhase(Long userId, Long projectId, Long deliverableId, CreatePhaseRequest request) {
        try {
            log.info(Constant.LOG_CREATING_PHASE, deliverableId, projectId, userId);

            // Authorization: Only OWNER can create phases
            authHelper.requireOwner(projectId, userId);

            // Verify deliverable exists and belongs to project
            PM_Deliverable deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_IN_PROJECT);
            }

            // Get project and increment phase counter
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

            Long newCounter = project.getPhaseCounter() + 1;
            project.setPhaseCounter(newCounter);
            projectRepository.save(project);

            // Generate key (PHA-01, PHA-02, etc.)
            String key = String.format("PHA-%02d", newCounter);

            // Get next order value
            Integer maxOrder = phaseRepository.findMaxOrderByDeliverableId(deliverableId);
            Integer nextOrder = maxOrder + 1;

            // Create phase
            PM_Phase phase = new PM_Phase();
            phase.setDeliverableId(deliverableId);
            phase.setName(request.getName());
            phase.setKey(key);
            phase.setOrder(nextOrder);

            PM_Phase savedPhase = phaseRepository.save(phase);

            log.info(Constant.LOG_PHASE_CREATED, savedPhase.getPhaseId(), key, deliverableId);

            Map<String, Object> data = new HashMap<>();
            data.put("phaseId", savedPhase.getPhaseId());
            data.put("key", savedPhase.getKey());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PHASE_CREATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to create phase for deliverable {} in project {} by user {}",
                    deliverableId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updatePhase(Long userId, Long projectId, Long phaseId, UpdatePhaseRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_PHASE, phaseId, projectId, userId);

            // Authorization: Only OWNER can update phases
            authHelper.requireOwner(projectId, userId);

            PM_Phase phase = phaseRepository.findById(phaseId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PHASE_NOT_FOUND));

            // Verify phase belongs to project
            PM_Deliverable deliverable = deliverableRepository.findById(phase.getDeliverableId())
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_PHASE_NOT_IN_PROJECT);
            }

            phase.setName(request.getName());
            phaseRepository.save(phase);

            log.info(Constant.LOG_PHASE_UPDATED, phaseId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PHASE_UPDATED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to update phase {} for project {} by user {}", phaseId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deletePhase(Long userId, Long projectId, Long phaseId) {
        try {
            log.info(Constant.LOG_DELETING_PHASE, phaseId, projectId, userId);

            // Authorization: Only OWNER can delete phases
            authHelper.requireOwner(projectId, userId);

            PM_Phase phase = phaseRepository.findById(phaseId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PHASE_NOT_FOUND));

            // Verify phase belongs to project
            PM_Deliverable deliverable = deliverableRepository.findById(phase.getDeliverableId())
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_DELIVERABLE_NOT_FOUND));

            if (!deliverable.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_PHASE_NOT_IN_PROJECT);
            }

            // Delete will cascade to tasks
            phaseRepository.delete(phase);

            log.info(Constant.LOG_PHASE_DELETED, phaseId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PHASE_DELETED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to delete phase {} for project {} by user {}", phaseId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }
}