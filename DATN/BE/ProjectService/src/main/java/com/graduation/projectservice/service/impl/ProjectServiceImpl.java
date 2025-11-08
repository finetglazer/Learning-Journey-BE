package com.graduation.projectservice.service;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.model.PM_Project;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.ProjectDTO;
import com.graduation.projectservice.payload.response.ProjectListResponse;
import com.graduation.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<ProjectListResponse> getUserProjects(Long userId) {
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
}