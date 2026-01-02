package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.exception.ForbiddenException;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.payload.request.CreateProjectRequest;
import com.graduation.projectservice.payload.request.ReorderRequest;
import com.graduation.projectservice.payload.request.UpdateProjectRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.ProjectDTO;
import com.graduation.projectservice.payload.response.ProjectListResponse;
import com.graduation.projectservice.repository.*;
import com.graduation.projectservice.service.ProjectMemberService;
import com.graduation.projectservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;
    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final ProjectAuthorizationHelper projectAuthorizationHelper;
    private final DeliverableRepository deliverableRepository;
    private final Random RAND = new Random();

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getUserProjects(Long userId) {
        log.info(Constant.LOG_RETRIEVING_PROJECTS, userId);

        List<PM_Project> projects = projectRepository.findActiveProjectsByUserId(userId);

        log.info(Constant.LOG_PROJECTS_FOUND, projects.size(), userId);

        List<ProjectDTO> projectDTOs = projects.stream()
                .map(project -> new ProjectDTO(project.getProjectId(), project.getName(), getRandomHexColor()))
                .collect(Collectors.toList());

        ProjectListResponse data = new ProjectListResponse(projectDTOs);

        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.PROJECTS_RETRIEVED_SUCCESS,
                data);
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
                    savedProject.getProjectId());

        } catch (Exception e) {
            log.error("Failed to create project for user {}", userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
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
                    null);
        } catch (Exception e) {
            log.error("Failed to update project {} for user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
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
                    null);
        } catch (Exception e) {
            log.error("Failed to delete project {} for user {}", projectId, userId, e);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
        }

    }

    @Override
    @Transactional
    public BaseResponse<?> reorderList(Long userId, Long projectId, ReorderRequest request) {
        try {
            log.info("User {} reordering list of type {} for project {}", userId, request.getType(), projectId);

            // 1. Authorization: Throws ForbiddenException if not owner
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // 2. Logic
            switch (request.getType()) {
                case TASK:
                    reorderTasks(request.getParentId(), request.getOrderedIds());
                    break;
                case PHASE:
                    reorderPhases(request.getParentId(), request.getOrderedIds());
                    break;
                case DELIVERABLE:
                    if (!request.getParentId().equals(projectId)) {
                        log.warn("Mismatched projectId in reorder request. URL: {}, Body: {}",
                                projectId, request.getParentId());
                        throw new ForbiddenException("Parent ID does not match Project ID.");
                    }
                    reorderDeliverables(request.getParentId(), request.getOrderedIds());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported reorder type: " + request.getType());
            }

            // 3. Response
            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    "Order updated",
                    null);

        } catch (Exception e) {
            log.error("Failed to reorder list for project {}: {}", projectId, e.getMessage(), e);
            // Return error using your BaseResponse pattern
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    e.getMessage(),
                    null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getInviteableProjects(Long userId, Long invitedId) {
        try {
            log.info("Fetching inviteable projects: userId={}, invitedId={}", userId, invitedId);

            // 1. Get all project IDs where the current user (userId) is a member
            // Note: Filter for MEMBER or OWNER to ensure they have permission to see the project
            List<Long> userProjectIds = projectMemberRepository.findAllByUserId(userId).stream()
                    .filter(m -> m.getRole() == ProjectMembershipRole.MEMBER || m.getRole() == ProjectMembershipRole.OWNER)
                    .map(PM_ProjectMember::getProjectId)
                    .toList();

            if (userProjectIds.isEmpty()) {
                return new BaseResponse<>(1, "No projects found for the user", List.of());
            }

            // 2. Get all project IDs where the invitedId is ALREADY present (any role: INVITED, MEMBER, etc.)
            List<Long> invitedMemberProjectIds = projectMemberRepository.findAllByUserId(invitedId).stream()
                    .map(PM_ProjectMember::getProjectId)
                    .toList();

            // 3. Filter: Keep project IDs that are in userProjectIds but NOT in invitedMemberProjectIds
            List<Long> inviteableProjectIds = userProjectIds.stream()
                    .filter(id -> !invitedMemberProjectIds.contains(id))
                    .collect(Collectors.toList());

            if (inviteableProjectIds.isEmpty()) {
                return new BaseResponse<>(1, "Target user is already a member of all your projects", List.of());
            }

            // 4. Fetch the actual project details
            List<PM_Project> projects = projectRepository.findAllById(inviteableProjectIds);

            // 5. Map to a clean response (you can use a specific DTO here)
            List<Map<String, Object>> response = projects.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("projectId", p.getProjectId());
                map.put("projectName", p.getName());
                return map;
            }).collect(Collectors.toList());

            log.info("Found {} inviteable projects for invitedId {}", response.size(), invitedId);
            return new BaseResponse<>(1, "Inviteable projects retrieved", response);

        } catch (Exception e) {
            log.error("Failed to fetch inviteable projects: {}", e.getMessage(), e);
            return new BaseResponse<>(0, "Error retrieving projects: " + e.getMessage(), null);
        }
    }

    /**
     * Private helper to reorder Tasks
     */
    private void reorderTasks(Long phaseId, List<Long> orderedIds) {
        // 1. Fetch all tasks that *actually* belong to this phase
        List<PM_Task> tasks = taskRepository.findByPhaseIdOrderByOrderAsc(phaseId);

        // 2. Check if the number of tasks found matches the number of IDs sent
        if (tasks.size() != orderedIds.size()) {
            throw new IllegalArgumentException(
                    "Reorder failed: The number of IDs sent (" + orderedIds.size() +
                            ") does not match the number of tasks in this phase (" + tasks.size() + ").");
        }

        // 3. Create a map of the tasks belonging to this phase for efficient lookup
        Map<Long, PM_Task> taskMap = tasks.stream()
                .collect(Collectors.toMap(PM_Task::getTaskId, task -> task));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long taskId = orderedIds.get(i);
            PM_Task task = taskMap.get(taskId);

            // 4. Check if the task from the request belongs to this phase
            if (task == null) {
                log.warn("Reorder failed: Task {} does not belong to phase {}.", taskId, phaseId);
                throw new IllegalArgumentException(
                        "Reorder failed: Task with ID " + taskId + " does not belong to this phase.");
            }

            // 5. Set the new order
            task.setOrder(i);
        }

        taskRepository.saveAll(tasks);
        log.info("Successfully reordered {} tasks for phase {}", tasks.size(), phaseId);
    }

    /**
     * Private helper to reorder Phases
     */
    private void reorderPhases(Long deliverableId, List<Long> orderedIds) {
        // 1. Fetch all phases for the deliverable
        List<PM_Phase> phases = phaseRepository.findByDeliverableIdOrderByOrderAsc(deliverableId);

        // 2. Check counts
        if (phases.size() != orderedIds.size()) {
            throw new IllegalArgumentException(
                    "Reorder failed: The number of IDs sent (" + orderedIds.size() +
                            ") does not match the number of phases in this deliverable (" + phases.size() + ").");
        }

        // 3. Create map
        Map<Long, PM_Phase> phaseMap = phases.stream()
                .collect(Collectors.toMap(PM_Phase::getPhaseId, phase -> phase));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long phaseId = orderedIds.get(i);
            PM_Phase phase = phaseMap.get(phaseId);

            // 4. Check ownership
            if (phase == null) {
                log.warn("Reorder failed: Phase {} does not belong to deliverable {}.", phaseId, deliverableId);
                throw new IllegalArgumentException(
                        "Reorder failed: Phase with ID " + phaseId + " does not belong to this deliverable.");
            }

            // 5. Set order
            phase.setOrder(i);
        }

        phaseRepository.saveAll(phases);
        log.info("Successfully reordered {} phases for deliverable {}", phases.size(), deliverableId);
    }

    /**
     * Private helper to reorder Deliverables
     */
    private void reorderDeliverables(Long projectId, List<Long> orderedIds) {
        // 1. Fetch all deliverables for the project
        List<PM_Deliverable> deliverables = deliverableRepository.findByProjectIdOrderByOrderAsc(projectId);

        // 2. Check counts
        if (deliverables.size() != orderedIds.size()) {
            throw new IllegalArgumentException(
                    "Reorder failed: The number of IDs sent (" + orderedIds.size() +
                            ") does not match the number of deliverables in this project (" + deliverables.size()
                            + ").");
        }

        // 3. Create map
        Map<Long, PM_Deliverable> deliverableMap = deliverables.stream()
                .collect(Collectors.toMap(PM_Deliverable::getDeliverableId, deliverable -> deliverable));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long deliverableId = orderedIds.get(i);
            PM_Deliverable deliverable = deliverableMap.get(deliverableId);

            // 4. Check ownership
            if (deliverable == null) {
                log.warn("Reorder failed: Deliverable {} does not belong to project {}.", deliverableId, projectId);
                throw new IllegalArgumentException(
                        "Reorder failed: Deliverable with ID " + deliverableId + " does not belong to this project.");
            }

            // 5. Set order
            deliverable.setOrder(i);
        }

        deliverableRepository.saveAll(deliverables);
        log.info("Successfully reordered {} deliverables for project {}", deliverables.size(), projectId);
    }

    /**
     * Generates a random hex color code.
     * This string can be used directly in a CSS 'backgroundColor' property.
     *
     * @return A 7-character hex color string (e.g., "#FF5733").
     */
    private String getRandomHexColor() {
        // Generate three random values for Red, Green, and Blue
        // Each value is between 0 (inclusive) and 255 (inclusive)
        int r = RAND.nextInt(256);
        int g = RAND.nextInt(256);
        int b = RAND.nextInt(256);

        // Format the integers as a 6-digit hex string, padded with zeros
        // and prefixed with a '#'
        return String.format("#%02X%02X%02X", r, g, b);
    }
}