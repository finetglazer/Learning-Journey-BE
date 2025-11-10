package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Milestone;
import com.graduation.projectservice.payload.request.CreateMilestoneRequest;
import com.graduation.projectservice.payload.request.UpdateMilestoneRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.repository.MilestoneRepository;
import com.graduation.projectservice.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneServiceImpl implements MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectAuthorizationHelper authHelper;

    @Override
    @Transactional
    public BaseResponse<?> createMilestone(Long userId, Long projectId, CreateMilestoneRequest request) {
        try {
            log.info(Constant.LOG_CREATING_MILESTONE, projectId, userId);

            // Authorization: Only OWNER can create milestones
            authHelper.requireOwner(projectId, userId);

            PM_Milestone milestone = new PM_Milestone();
            milestone.setProjectId(projectId);
            milestone.setName(request.getName());
            milestone.setDate(request.getDate());

            PM_Milestone savedMilestone = milestoneRepository.save(milestone);

            log.info(Constant.LOG_MILESTONE_CREATED, savedMilestone.getMilestoneId(), projectId);

            Map<String, Object> data = new HashMap<>();
            data.put("milestoneId", savedMilestone.getMilestoneId());

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.MILESTONE_CREATED_SUCCESS,
                    data
            );

        } catch (Exception e) {
            log.error("Failed to create milestone for project {} by user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateMilestone(Long userId, Long projectId, Long milestoneId, UpdateMilestoneRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_MILESTONE, milestoneId, projectId, userId);

            // Authorization: Only OWNER can update milestones
            authHelper.requireOwner(projectId, userId);

            PM_Milestone milestone = milestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_MILESTONE_NOT_FOUND));

            if (!milestone.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_MILESTONE_NOT_IN_PROJECT);
            }

            milestone.setName(request.getName());
            milestone.setDate(request.getDate());
            milestoneRepository.save(milestone);

            log.info(Constant.LOG_MILESTONE_UPDATED, milestoneId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.MILESTONE_UPDATED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to update milestone {} for project {} by user {}", milestoneId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteMilestone(Long userId, Long projectId, Long milestoneId) {
        try {
            log.info(Constant.LOG_DELETING_MILESTONE, milestoneId, projectId, userId);

            // Authorization: Only OWNER can delete milestones
            authHelper.requireOwner(projectId, userId);

            PM_Milestone milestone = milestoneRepository.findById(milestoneId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_MILESTONE_NOT_FOUND));

            if (!milestone.getProjectId().equals(projectId)) {
                throw new RuntimeException(Constant.ERROR_MILESTONE_NOT_IN_PROJECT);
            }

            milestoneRepository.delete(milestone);

            log.info(Constant.LOG_MILESTONE_DELETED, milestoneId, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.MILESTONE_DELETED_SUCCESS,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to delete milestone {} for project {} by user {}", milestoneId, projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }
}