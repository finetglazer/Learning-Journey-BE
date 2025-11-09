package com.graduation.projectservice.constant;

public class Constant {

    // ============================================
    // Response Messages
    // ============================================
    public static final String PROJECTS_RETRIEVED_SUCCESS = "Projects retrieved successfully";
    public static final String PROJECT_CREATED_SUCCESS = "Project created successfully";
    public static final String PROJECT_UPDATED_SUCCESS = "Project updated";
    public static final String PROJECT_DELETED_SUCCESS = "Project deleted";

    // ============================================
    // Response Status Codes
    // ============================================
    public static final int SUCCESS_STATUS = 1;
    public static final int ERROR_STATUS = 0;

    // ============================================
    // Log Messages
    // ============================================
    public static final String LOG_RETRIEVING_PROJECTS = "Retrieving projects for user: {}";
    public static final String LOG_PROJECTS_FOUND = "Found {} projects for user {}";
    public static final String LOG_GET_PROJECTS_REQUEST = "GET /api/pm/projects - User: {}";
    public static final String LOG_CREATING_PROJECT = "Creating new project for user: {}";
    public static final String LOG_PROJECT_CREATED = "Project created with ID: {} for user: {}";
    public static final String LOG_UPDATING_PROJECT = "Updating project {} by user: {}";
    public static final String LOG_PROJECT_UPDATED = "Project {} updated successfully";
    public static final String LOG_DELETING_PROJECT = "Deleting project {} by user: {}";
    public static final String LOG_PROJECT_DELETED = "Project {} deleted successfully";
    public static final String LOG_POST_PROJECT_REQUEST = "POST /api/pm/projects - User: {}";
    public static final String LOG_PUT_PROJECT_REQUEST = "PUT /api/pm/projects/{} - User: {}";
    public static final String LOG_DELETE_PROJECT_REQUEST = "DELETE /api/pm/projects/{} - User: {}";

    // ============================================
    // Error Messages
    // ============================================
    public static final String ERROR_PROJECT_NOT_FOUND = "Project not found";
    public static final String ERROR_UNAUTHORIZED_ACCESS = "You don't have permission to access this project";
    public static final String ERROR_INVALID_REQUEST = "Invalid request";
    public static final String ERROR_PROJECT_NAME_REQUIRED = "Project name is required";

    // ============================================
    // Database Constraints
    // ============================================
    public static final long DEFAULT_COUNTER_VALUE = 0L;

    // ============================================
    // Project Member Roles
    // ============================================
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_INVITED = "INVITED";
}