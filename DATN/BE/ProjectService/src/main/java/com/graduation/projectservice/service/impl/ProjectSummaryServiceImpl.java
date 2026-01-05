package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.model.enums.RiskStatus;
import com.graduation.projectservice.model.enums.TaskStatus;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.*;
import com.graduation.projectservice.service.ProjectSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectSummaryServiceImpl implements ProjectSummaryService {

    private final DeliverableRepository deliverableRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectAuthorizationHelper projectAuthorizationHelper;
    private final UserServiceClient userServiceClient;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final RiskRepository riskRepository;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getDeliverableProgress(Long userId, Long projectId) {
        try {
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // [id, name, key, totalTasks, completedTasks]
            List<Object[]> results = deliverableRepository.findDeliverableProgressStats(projectId);
            List<DeliverableProgressDTO> progressList = new ArrayList<>();

            for (Object[] row : results) {
                Long totalTasks = (Long) row[3];
                Long completedTasks = (Long) row[4];
                int percentage = (totalTasks != null && totalTasks > 0)
                        ? (int) ((completedTasks * 100) / totalTasks) : 0;

                progressList.add(new DeliverableProgressDTO(
                        (Long) row[0], (String) row[1], (String) row[2], percentage
                ));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("progress", progressList);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Deliverable progress retrieved", data);

        } catch (Exception e) {
            log.error("Error getting deliverable progress: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getTeammateWorkload(Long userId, Long projectId) {
        try {
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // 1. Get all members
            List<PM_ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);

            // 2. Get task counts per user (internal calculation only)
            List<Object[]> taskCounts = taskAssigneeRepository.countTasksByUserInProject(projectId);
            Map<Long, Long> countMap = taskCounts.stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

            // 3. Batch fetch User Names
            List<Long> memberIds = members.stream()
                    .map(PM_ProjectMember::getUserId)
                    .collect(Collectors.toList());

            List<UserBatchDTO> userDetails = userServiceClient.findUsersByIds(memberIds);
            Map<Long, String> userNameMap = userDetails.stream()
                    .collect(Collectors.toMap(UserBatchDTO::getUserId, UserBatchDTO::getName, (a, b) -> b));

            // 4. Calculate logic
            long totalTasksInProject = countMap.values().stream().mapToLong(Long::longValue).sum();
            List<TeammateWorkloadDTO> workloadList = new ArrayList<>();

            for (PM_ProjectMember member : members) {
                Long mUserId = member.getUserId();
                long assignedCount = countMap.getOrDefault(mUserId, 0L);

                int percentage = (totalTasksInProject > 0)
                        ? (int) ((assignedCount * 100) / totalTasksInProject) : 0;

                String name = userNameMap.getOrDefault(mUserId, "Unknown Member");

                // Add ONLY name and percentage
                workloadList.add(new TeammateWorkloadDTO(name, percentage));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("workload", workloadList);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Teammate workload retrieved", data);

        } catch (Exception e) {
            log.error("Error getting teammate workload: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    // ... [getTaskStats method remains unchanged] ...
    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getTaskStats(Long userId, Long projectId) {
        try {
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            List<PM_Task> tasks = taskRepository.findAllByProjectId(projectId);

            long todo = 0, inProgress = 0, inReview = 0, done = 0;
            long completed = 0, dueSoon = 0, overdue = 0, unassigned = 0;
            LocalDate today = LocalDate.now();
            LocalDate threeDaysLater = today.plusDays(3);

            for (PM_Task task : tasks) {
                // Status Counters
                if (task.getStatus() != null) {
                    switch (task.getStatus()) {
                        case TO_DO -> todo++;
                        case IN_PROGRESS -> inProgress++;
                        case IN_REVIEW -> inReview++;
                        case DONE -> done++;
                    }
                }

                // Deadline Counters
                if (task.getAssignees() == null || task.getAssignees().isEmpty()) unassigned++;

                if (task.getStatus() == TaskStatus.DONE) {
                    completed++;
                } else if (task.getEndDate() != null) {
                    if (task.getEndDate().isBefore(today)) overdue++;
                    else if (!task.getEndDate().isAfter(threeDaysLater)) dueSoon++;
                }
            }

            TaskStatsDTO statsDTO = new TaskStatsDTO(
                    new TaskStatsDTO.StatsByStatus(todo, inProgress, inReview, done),
                    new TaskStatsDTO.StatsByDeadline(completed, dueSoon, overdue, unassigned)
            );

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Task stats retrieved", statsDTO);

        } catch (Exception e) {
            log.error("Error getting task stats: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getProjectTimeline(Long userId, Long projectId) {
        try {
            // 1. Auth Check
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // 2. Fetch Project Info (for Start Date)
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // 3. Fetch Milestones (Ordered by date ASC)
            List<PM_Milestone> milestones = milestoneRepository.findAllByProjectIdOrderByDateAsc(projectId);

            // 4. Map to DTO
            List<TimelineMilestoneDTO> milestoneDTOs = milestones.stream()
                    .map(m -> new TimelineMilestoneDTO(m.getMilestoneId(), m.getName(), m.getDate()))
                    .collect(Collectors.toList());

            TimelineResponseDTO responseDTO = new TimelineResponseDTO(
                    project.getStartDate(),
                    LocalDate.now(),
                    milestoneDTOs
            );

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Project timeline retrieved", responseDTO);

        } catch (Exception e) {
            log.error("Error getting timeline: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getActiveRisks(Long userId, Long projectId) {
        try {
            // 1. Auth Check
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // 2. Fetch all UNRESOLVED risks
            List<PM_Risk> unresolvedRisks = riskRepository.findByProjectIdAndStatus(projectId, RiskStatus.UNRESOLVED);

            // 3. Sort by "Impact Score" (Probability * Impact) Descending
            // Assuming Enum has .getValue(). If not, adapt based on Enum structure.
            unresolvedRisks.sort((r1, r2) -> {
                int score1 = r1.getProbability().getValue() * r1.getImpact().getValue();
                int score2 = r2.getProbability().getValue() * r2.getImpact().getValue();
                return Integer.compare(score2, score1); // Descending
            });

            // 4. Take top 5
            int totalCount = unresolvedRisks.size();
            List<PM_Risk> topRisks = unresolvedRisks.stream().limit(5).collect(Collectors.toList());
            int displayCount = topRisks.size();

            // 5. Map to DTO
            List<ActiveRiskItemDTO> riskItems = topRisks.stream()
                    .map(r -> new ActiveRiskItemDTO(r.getKey(), r.getRiskStatement()))
                    .collect(Collectors.toList());

            ActiveRiskSummaryDTO responseDTO = new ActiveRiskSummaryDTO(totalCount, displayCount, riskItems);

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Active risks retrieved", responseDTO);

        } catch (Exception e) {
            log.error("Error getting active risks: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getProjectDashboardSummary(Long userId, Long projectId) {
        try {
            // 1. Single Auth Check for the whole dashboard
            projectAuthorizationHelper.requireActiveMember(projectId, userId);

            // --- A. Deliverable Progress ---
            List<Object[]> delResults = deliverableRepository.findDeliverableProgressStats(projectId);
            List<DeliverableProgressDTO> progressList = new ArrayList<>();
            for (Object[] row : delResults) {
                Long total = (Long) row[3];
                Long completed = (Long) row[4];
                int pct = (total != null && total > 0) ? (int) ((completed * 100) / total) : 0;
                progressList.add(new DeliverableProgressDTO((Long) row[0], (String) row[1], (String) row[2], pct));
            }

            // --- B. Teammate Workload ---
            List<PM_ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);
            List<Object[]> taskCounts = taskAssigneeRepository.countTasksByUserInProject(projectId);
            Map<Long, Long> countMap = taskCounts.stream()
                    .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

            // Batch Fetch User Names
            List<Long> memberIds = members.stream().map(PM_ProjectMember::getUserId).collect(Collectors.toList());
            List<UserBatchDTO> users = userServiceClient.findUsersByIds(memberIds);
            Map<Long, String> nameMap = users.stream()
                    .collect(Collectors.toMap(UserBatchDTO::getUserId, UserBatchDTO::getName, (a, b) -> b));

            long totalTasks = countMap.values().stream().mapToLong(Long::longValue).sum();
            List<TeammateWorkloadDTO> workloadList = members.stream().map(m -> {
                long assigned = countMap.getOrDefault(m.getUserId(), 0L);
                int pct = (totalTasks > 0) ? (int) ((assigned * 100) / totalTasks) : 0;
                return new TeammateWorkloadDTO(nameMap.getOrDefault(m.getUserId(), "Unknown"), pct);
            }).collect(Collectors.toList());

            // --- C. Task Stats ---
            List<PM_Task> tasks = taskRepository.findAllByProjectId(projectId);
            long todo = 0, inProgress = 0, inReview = 0, done = 0;
            long completedStat = 0, dueSoon = 0, overdue = 0, unassigned = 0;
            LocalDate today = LocalDate.now();
            LocalDate threeDaysLater = today.plusDays(3);

            for (PM_Task task : tasks) {
                if (task.getStatus() != null) {
                    switch (task.getStatus()) {
                        case TO_DO -> todo++;
                        case IN_PROGRESS -> inProgress++;
                        case IN_REVIEW -> inReview++;
                        case DONE -> done++;
                    }
                }
                if (task.getAssignees() == null || task.getAssignees().isEmpty()) unassigned++;

                if (task.getStatus() == TaskStatus.DONE) {
                    completedStat++;
                } else if (task.getEndDate() != null) {
                    if (task.getEndDate().isBefore(today)) overdue++;
                    else if (!task.getEndDate().isAfter(threeDaysLater)) dueSoon++;
                }
            }
            TaskStatsDTO taskStats = new TaskStatsDTO(
                    new TaskStatsDTO.StatsByStatus(todo, inProgress, inReview, done),
                    new TaskStatsDTO.StatsByDeadline(completedStat, dueSoon, overdue, unassigned)
            );

            // --- D. Timeline ---
            PM_Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            List<PM_Milestone> milestones = milestoneRepository.findAllByProjectIdOrderByDateAsc(projectId);
            List<TimelineMilestoneDTO> timelineDTOs = milestones.stream()
                    .map(m -> new TimelineMilestoneDTO(m.getMilestoneId(), m.getName(), m.getDate()))
                    .collect(Collectors.toList());
            TimelineResponseDTO timeline = new TimelineResponseDTO(project.getStartDate(), LocalDate.now(), timelineDTOs);

            // --- E. Active Risks ---
            List<PM_Risk> risks = riskRepository.findByProjectIdAndStatus(projectId, RiskStatus.UNRESOLVED);
            // Sort by Impact Score (Prob * Impact) Descending
            risks.sort((r1, r2) -> Integer.compare(
                    r2.getProbability().getValue() * r2.getImpact().getValue(),
                    r1.getProbability().getValue() * r1.getImpact().getValue()
            ));

            List<ActiveRiskItemDTO> topRisks = risks.stream().limit(5)
                    .map(r -> new ActiveRiskItemDTO(r.getKey(), r.getRiskStatement()))
                    .collect(Collectors.toList());
            ActiveRiskSummaryDTO riskSummary = new ActiveRiskSummaryDTO(risks.size(), topRisks.size(), topRisks);

            // 3. Construct Final Response
            ProjectDashboardSummaryDTO dashboardData = new ProjectDashboardSummaryDTO(
                    progressList,
                    workloadList,
                    taskStats,
                    timeline,
                    riskSummary
            );

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Project dashboard summary retrieved", dashboardData);

        } catch (Exception e) {
            log.error("Error getting dashboard summary: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        }
    }
}