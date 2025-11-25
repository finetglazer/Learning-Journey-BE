package com.graduation.projectservice.repository;

import java.time.LocalDate;

public interface TaskProjectProjection {
    Long getTaskId();
    String getTaskName();
    LocalDate getEndDate();
    Long getProjectId();
    String getProjectName();
}