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

    // Milestone Log Messages
    public static final String LOG_CREATING_MILESTONE = "Creating milestone for project {} by user {}";
    public static final String LOG_MILESTONE_CREATED = "Milestone {} created for project {}";
    public static final String LOG_UPDATING_MILESTONE = "Updating milestone {} for project {} by user {}";
    public static final String LOG_MILESTONE_UPDATED = "Milestone {} updated for project {}";
    public static final String LOG_DELETING_MILESTONE = "Deleting milestone {} for project {} by user {}";
    public static final String LOG_MILESTONE_DELETED = "Milestone {} deleted for project {}";
    public static final String LOG_POST_MILESTONE_REQUEST = "POST /api/pm/projects/{}/milestones request received from user {}";
    public static final String LOG_PUT_MILESTONE_REQUEST = "PUT /api/pm/projects/{}/milestones/{} request received from user {}";
    public static final String LOG_DELETE_MILESTONE_REQUEST = "DELETE /api/pm/projects/{}/milestones/{} request received from user {}";

    // Milestone Success Messages
    public static final String MILESTONE_CREATED_SUCCESS = "Milestone created";
    public static final String MILESTONE_UPDATED_SUCCESS = "Milestone updated";
    public static final String MILESTONE_DELETED_SUCCESS = "Milestone deleted";

    // Milestone Error Messages
    public static final String ERROR_MILESTONE_NOT_FOUND = "Milestone not found";
    public static final String ERROR_MILESTONE_NOT_IN_PROJECT = "Milestone does not belong to this project";

    // Deliverable Log Messages
    public static final String LOG_CREATING_DELIVERABLE = "Creating deliverable for project {} by user {}";
    public static final String LOG_DELIVERABLE_CREATED = "Deliverable {} (key: {}) created for project {}";
    public static final String LOG_UPDATING_DELIVERABLE = "Updating deliverable {} for project {} by user {}";
    public static final String LOG_DELIVERABLE_UPDATED = "Deliverable {} updated for project {}";
    public static final String LOG_DELETING_DELIVERABLE = "Deleting deliverable {} for project {} by user {}";
    public static final String LOG_DELIVERABLE_DELETED = "Deliverable {} deleted for project {}";
    public static final String LOG_POST_DELIVERABLE_REQUEST = "POST /api/pm/projects/{}/deliverables request received from user {}";
    public static final String LOG_PUT_DELIVERABLE_REQUEST = "PUT /api/pm/projects/{}/deliverables/{} request received from user {}";
    public static final String LOG_DELETE_DELIVERABLE_REQUEST = "DELETE /api/pm/projects/{}/deliverables/{} request received from user {}";

    // Deliverable Success Messages
    public static final String DELIVERABLE_CREATED_SUCCESS = "Deliverable created";
    public static final String DELIVERABLE_UPDATED_SUCCESS = "Deliverable updated";
    public static final String DELIVERABLE_DELETED_SUCCESS = "Deliverable deleted";

    // Deliverable Error Messages
    public static final String ERROR_DELIVERABLE_NOT_FOUND = "Deliverable not found";
    public static final String ERROR_DELIVERABLE_NOT_IN_PROJECT = "Deliverable does not belong to this project";

    // Phase Log Messages
    public static final String LOG_CREATING_PHASE = "Creating phase for deliverable {} in project {} by user {}";
    public static final String LOG_PHASE_CREATED = "Phase {} (key: {}) created for deliverable {}";
    public static final String LOG_UPDATING_PHASE = "Updating phase {} for project {} by user {}";
    public static final String LOG_PHASE_UPDATED = "Phase {} updated for project {}";
    public static final String LOG_DELETING_PHASE = "Deleting phase {} for project {} by user {}";
    public static final String LOG_PHASE_DELETED = "Phase {} deleted for project {}";
    public static final String LOG_POST_PHASE_REQUEST = "POST /api/pm/projects/{}/deliverables/{}/phases request received from user {}";
    public static final String LOG_PUT_PHASE_REQUEST = "PUT /api/pm/projects/{}/phases/{} request received from user {}";
    public static final String LOG_DELETE_PHASE_REQUEST = "DELETE /api/pm/projects/{}/phases/{} request received from user {}";

    // Phase Success Messages
    public static final String PHASE_CREATED_SUCCESS = "Phase created";
    public static final String PHASE_UPDATED_SUCCESS = "Phase updated";
    public static final String PHASE_DELETED_SUCCESS = "Phase deleted";

    // Phase Error Messages
    public static final String ERROR_PHASE_NOT_FOUND = "Phase not found";
    public static final String ERROR_PHASE_NOT_IN_PROJECT = "Phase does not belong to this project";

    // Task Log Messages
    public static final String LOG_CREATING_TASK = "Creating task for phase {} in project {} by user {}";
    public static final String LOG_TASK_CREATED = "Task {} (key: {}) created for phase {}";
    public static final String LOG_UPDATING_TASK = "Updating task {} for project {} by user {}";
    public static final String LOG_TASK_UPDATED = "Task {} updated for project {}";
    public static final String LOG_DELETING_TASK = "Deleting task {} for project {} by user {}";
    public static final String LOG_TASK_DELETED = "Task {} deleted for project {}";
    public static final String LOG_UPDATING_TASK_STATUS = "Updating status for task {} in project {} by user {}";
    public static final String LOG_TASK_STATUS_UPDATED = "Task {} status updated to '{}' for project {}";
    public static final String LOG_POST_TASK_REQUEST = "POST /api/pm/projects/{}/phases/{}/tasks request received from user {}";
    public static final String LOG_PUT_TASK_REQUEST = "PUT /api/pm/projects/{}/tasks/{} request received from user {}";
    public static final String LOG_DELETE_TASK_REQUEST = "DELETE /api/pm/projects/{}/tasks/{} request received from user {}";
    public static final String LOG_PUT_TASK_STATUS_REQUEST = "PUT /api/pm/projects/{}/tasks/{}/status request received from user {}";

    // Task Success Messages
    public static final String TASK_CREATED_SUCCESS = "Task created";
    public static final String TASK_UPDATED_SUCCESS = "Task updated";
    public static final String TASK_DELETED_SUCCESS = "Task deleted";
    public static final String TASK_STATUS_UPDATED_SUCCESS = "Task status updated";

    // Task Error Messages
    public static final String ERROR_TASK_NOT_FOUND = "Task not found";
    public static final String ERROR_TASK_NOT_IN_PROJECT = "Task does not belong to this project";

    // Project Structure Log Messages
    public static final String LOG_GET_PROJECT_STRUCTURE_REQUEST = "GET /api/pm/projects/{}/structure request received from user {}";
    public static final String LOG_RETRIEVING_PROJECT_STRUCTURE = "Retrieving project structure for project {} by user {}";
    public static final String LOG_PROJECT_STRUCTURE_RETRIEVED = "Project structure retrieved for project {}. Found {} deliverables";

    // Project Structure Success Messages
    public static final String PROJECT_STRUCTURE_RETRIEVED_SUCCESS = "Project structure retrieved";

    // Phase Tasks Log Messages
    public static final String LOG_GET_PHASE_TASKS_REQUEST = "GET /api/pm/projects/{}/phases/{}/tasks request received from user {}";
    public static final String LOG_RETRIEVING_PHASE_TASKS = "Retrieving tasks for phase {} in project {} by user {}";
    public static final String LOG_PHASE_TASKS_RETRIEVED = "Tasks retrieved for phase {}. Found {} tasks";

    // Phase Tasks Success Messages
    public static final String PHASE_TASKS_RETRIEVED_SUCCESS = "Tasks for phase {} retrieved";
}