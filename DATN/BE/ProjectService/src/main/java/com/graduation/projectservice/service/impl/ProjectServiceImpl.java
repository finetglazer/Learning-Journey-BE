package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.model.PM_ProjectMember;
import com.graduation.projectservice.model.ProjectMemberKey;
import com.graduation.projectservice.model.ProjectMembershipRole;
import com.graduation.projectservice.payload.request.CreateProjectRequest;
import com.graduation.projectservice.payload.request.UpdateProjectRequest;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.ProjectMemberRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getUserProjects(Long userId) {
        log.info(Constant.LOG_RETRIEVING_PROJECTS, userId);

        List<PM_Project> projects = projectRepository.findAllByUserId(userId);

        log.info(Constant.LOG_PROJECTS_FOUND, projects.size(), userId);

        List<ProjectDTO> projectDTOs = projects.stream()
                .map(project -> new ProjectDTO(project.getProjectId(), project.getName()))
                .collect(Collectors.toList());

        ProjectListResponse data = new ProjectListResponse(projectDTOs);

        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.PROJECTS_RETRIEVED_SUCCESS,
                data
        );
    }

    @Override
    @Transactional
    public BaseResponse<?> createProject(Long userId, CreateProjectRequest request) {
        try {
            log.info(Constant.LOG_CREATING_PROJECT, userId);

            // Create new project
            PM_Project project = new PM_Project();
            project.setName(request.getName());
            project.setStartDate(LocalDate.now());
            project.setTaskCounter(Constant.DEFAULT_COUNTER_VALUE);
            project.setRiskCounter(Constant.DEFAULT_COUNTER_VALUE);
            project.setDeliverableCounter(Constant.DEFAULT_COUNTER_VALUE);
            project.setPhaseCounter(Constant.DEFAULT_COUNTER_VALUE);

            PM_Project savedProject = projectRepository.save(project);

            // Create project member entry with OWNER role
            PM_ProjectMember projectMember = new PM_ProjectMember();
            projectMember.setProjectId(savedProject.getProjectId());
            projectMember.setUserId(userId);
            projectMember.setRole(ProjectMembershipRole.OWNER);
            projectMember.setCustomRoleName(null);

            projectMemberRepository.save(projectMember);

            log.info(Constant.LOG_PROJECT_CREATED, savedProject.getProjectId(), userId);

            // Return projectId directly in data field
            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PROJECT_CREATED_SUCCESS,
                    savedProject.getProjectId()
            );

        } catch (Exception e) {
            log.error("Failed to create project for user {}", userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        try {
            log.info(Constant.LOG_UPDATING_PROJECT, projectId, userId);

            // Check if project exists
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

            // Check if user is OWNER
            ProjectMemberKey memberKey = new ProjectMemberKey(projectId, userId);
            PM_ProjectMember projectMember = projectMemberRepository.findById(memberKey)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_UNAUTHORIZED_ACCESS));

            if (projectMember.getRole() != ProjectMembershipRole.OWNER) {
                throw new RuntimeException(Constant.ERROR_UNAUTHORIZED_ACCESS);
            }

            // Update project name
            project.setName(request.getName());
            projectRepository.save(project);

            log.info(Constant.LOG_PROJECT_UPDATED, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PROJECT_UPDATED_SUCCESS,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to update project {} for user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }


    }

    @Override
    @Transactional
    public BaseResponse<?> deleteProject(Long userId, Long projectId) {
        try {


            log.info(Constant.LOG_DELETING_PROJECT, projectId, userId);

            // Check if project exists
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_PROJECT_NOT_FOUND));

            // Check if user is OWNER
            ProjectMemberKey memberKey = new ProjectMemberKey(projectId, userId);
            PM_ProjectMember projectMember = projectMemberRepository.findById(memberKey)
                    .orElseThrow(() -> new RuntimeException(Constant.ERROR_UNAUTHORIZED_ACCESS));

            if (projectMember.getRole() != ProjectMembershipRole.OWNER) {
                throw new RuntimeException(Constant.ERROR_UNAUTHORIZED_ACCESS);
            }

            // Delete project (cascade will delete associated data)
            projectRepository.delete(project);

            log.info(Constant.LOG_PROJECT_DELETED, projectId);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    Constant.PROJECT_DELETED_SUCCESS,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to delete project {} for user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null
            );
        }

    }
}