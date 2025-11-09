package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateProjectRequest;
import com.graduation.projectservice.payload.request.UpdateProjectRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface ProjectService {

    /**
     * Retrieve all projects for a given user
     */
    BaseResponse<?> getUserProjects(Long userId);

    /**
     * Create a new project and automatically assign the creator as OWNER
     */
    BaseResponse<?> createProject(Long userId, CreateProjectRequest request);

    BaseResponse<?> updateProject(Long userId, Long projectId, UpdateProjectRequest request);

    BaseResponse<?> deleteProject(Long userId, Long projectId);
}