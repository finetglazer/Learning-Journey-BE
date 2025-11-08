package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.ProjectListResponse;

public interface ProjectService {
    BaseResponse<ProjectListResponse> getUserProjects(Long userId);
}