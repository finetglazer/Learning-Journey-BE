package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.*;
import com.graduation.projectservice.model.enums.RiskLevel;
import com.graduation.projectservice.model.enums.RiskStatus;
import com.graduation.projectservice.payload.request.CreateRiskRequest;
import com.graduation.projectservice.payload.request.UpdateRiskRequest;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.ProjectMemberRepository;
import com.graduation.projectservice.repository.ProjectRepository;
import com.graduation.projectservice.repository.RiskAssigneeRepository;
import com.graduation.projectservice.repository.RiskRepository;
import com.graduation.projectservice.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskRepository riskRepository;
    private final RiskAssigneeRepository riskAssigneeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationHelper authHelper;
    private final UserServiceClient userServiceClient;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getRisks(Long userId, Long projectId, int page, int limit, String search, String assigneeFilter) {
        // 1. Auth: Owner or Member
        authHelper.requireActiveMember(projectId, userId);

        // 2. Prepare Filter
        Long filterUserId = null;
        if ("me".equalsIgnoreCase(assigneeFilter)) {
            filterUserId = userId;
        }

        // --- [FIX START] ---
        // Prevent 'lower(bytea)' error by ensuring search is never null.
        // If search is empty/null, passing "" results in 'LIKE %%' which matches everything.
        String searchKeyword = (search == null) ? "" : search;
        // --- [FIX END] ---

        // 3. Query DB
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("riskId").descending());

        // Use 'searchKeyword' instead of 'search'
        Page<PM_Risk> riskPage = riskRepository.searchRisks(projectId, searchKeyword, filterUserId, pageable);

        // 4. Collect User IDs for Batch Fetching
        Set<Long> userIdsToFetch = new HashSet<>();
        riskPage.getContent().forEach(risk -> {
            if (risk.getAssignees() != null) {
                risk.getAssignees().forEach(a -> userIdsToFetch.add(a.getUserId()));
            }
        });

        // 5. Fetch User Info from User Service
        List<UserBatchDTO> userDetails = userServiceClient.findUsersByIds(new ArrayList<>(userIdsToFetch));
        Map<Long, UserBatchDTO> userMap = userDetails.stream()
                .collect(Collectors.toMap(UserBatchDTO::getUserId, u -> u));

        // 6. Map to DTO
        List<RiskDTO> riskDTOS = riskPage.getContent().stream().map(risk -> {
            List<AssigneeDTO> assigneeDTOS = new ArrayList<>();
            if (risk.getAssignees() != null) {
                assigneeDTOS = risk.getAssignees().stream().map(a -> {
                    UserBatchDTO u = userMap.get(a.getUserId());
                    return new AssigneeDTO(
                            a.getUserId(),
                            u != null ? u.getAvatarUrl() : null
                    );
                }).collect(Collectors.toList());
            }

            int score = risk.getProbability().getValue() * risk.getImpact().getValue();
            String degree = getRiskDegree(score);

            return RiskDTO.builder()
                    .id(risk.getRiskId())
                    .key(risk.getKey())
                    .riskStatement(risk.getRiskStatement())
                    .probability(risk.getProbability().getLabel())
                    .impact(risk.getImpact().getLabel())
                    .status(risk.getStatus().name())
                    .riskScore(score)
                    .riskDegree(degree)
                    .assignees(assigneeDTOS)
                    .mitigationPlan(risk.getMitigationPlan())
                    .note(risk.getNote())
                    .revisedProbability(risk.getRevisedProbability() != null ? risk.getRevisedProbability().getLabel() : null)
                    .revisedImpact(risk.getRevisedImpact() != null ? risk.getRevisedImpact().getLabel() : null)
                    .build();
        }).collect(Collectors.toList());

        // 7. Build Response
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page);
        pagination.put("totalPages", riskPage.getTotalPages());
        pagination.put("totalRisks", riskPage.getTotalElements());

        data.put("pagination", pagination);
        data.put("risks", riskDTOS);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Risks (Page " + page + ") retrieved", data);
    }

    @Override
    @Transactional
    public BaseResponse<?> createRisk(Long userId, Long projectId, CreateRiskRequest request) {
        // 1. Auth & Project Check
        authHelper.requireActiveMember(projectId, userId);
        PM_Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // 2. Generate Key
        long newCount = project.getRiskCounter() + 1;
        String key = "R-" + String.format("%02d", newCount);

        // 3. Logic Validation & Defaults (Your Deduction)
        String statement = (request.getRisk_statement() == null || request.getRisk_statement().trim().isEmpty())
                ? "New Risk"
                : request.getRisk_statement();

        // Convert String "5-Very high" -> Enum VERY_HIGH. Default to VERY_LOW if null/invalid.
        RiskLevel prob = request.getProbability() != null
                ? RiskLevel.fromLabel(request.getProbability())
                : RiskLevel.VERY_LOW;

        RiskLevel imp = request.getImpact() != null
                ? RiskLevel.fromLabel(request.getImpact())
                : RiskLevel.VERY_LOW;

        // 4. Save Risk
        PM_Risk risk = new PM_Risk();
        risk.setProjectId(projectId);
        risk.setKey(key);
        risk.setRiskStatement(statement);
        risk.setProbability(prob);
        risk.setImpact(imp);

        // Force Defaults for Status and Revised fields
        risk.setStatus(RiskStatus.UNRESOLVED);
        risk.setRevisedProbability(null); // Option B: Start null
        risk.setRevisedImpact(null);      // Option B: Start null

        PM_Risk savedRisk = riskRepository.save(risk);

        // 6. Update Assignees (Logic with Validation)
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {

            // A. Validate: Only get IDs that are actual members of THIS project
            List<Long> validMemberIds = projectMemberRepository.findValidMemberIds(projectId, request.getAssigneeIds());

//             Optional: If you want to throw an error if ANY ID is invalid, do this:
             if (validMemberIds.size() != request.getAssigneeIds().size()) {
//                 throw new IllegalArgumentException("One or more assignees are not members of this project.");
                 return new BaseResponse<>(0, "One or more assignees are not members of this project.", null);
             }

            // B. Save only the valid ones
            if (!validMemberIds.isEmpty()) {
                List<PM_RiskAssignee> newAssignees = validMemberIds.stream()
                        .map(uid -> new PM_RiskAssignee(savedRisk.getRiskId(), uid, savedRisk))
                        .collect(Collectors.toList());
                riskAssigneeRepository.saveAll(newAssignees);
            }
        }

        // 7. Update Project Counter
        project.setRiskCounter(newCount);
        projectRepository.save(project);

        // 8. Response
        Map<String, Object> data = new HashMap<>();
        data.put("riskId", savedRisk.getRiskId());
        data.put("key", savedRisk.getKey());
        data.put("status", savedRisk.getStatus());

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Risk created", data);
    }
    @Override
    @Transactional
    public BaseResponse<?> updateRisk(Long userId, Long projectId, Long riskId, UpdateRiskRequest request) {
        // 1. Auth
        authHelper.requireActiveMember(projectId, userId);

        // 2. Fetch Risk
        if (riskRepository.findById(riskId).isEmpty()) {
            return new BaseResponse<>(0, "Risk not found", null);
        }

        PM_Risk risk = riskRepository.findById(riskId)
            .orElseThrow(() -> new RuntimeException("Risk not found"));


        if (!risk.getProjectId().equals(projectId)) {
            throw new RuntimeException("Risk does not belong to this project");
        }

        // --- [NEW] VALIDATION BLOCK MOVED HERE (Safety First) ---
        if (request.getAssignees() != null && !request.getAssignees().isEmpty()) {
            // Validate: Check if all IDs are valid members
            List<Long> validMemberIds = projectMemberRepository.findValidMemberIds(projectId, request.getAssignees());

            if (validMemberIds.size() != request.getAssignees().size()) {
                // We return early here. Since we haven't modified 'risk' yet,
                // nothing is saved to DB. Safe.
                return new BaseResponse<>(0, "One or more assignees are not members of this project.", null);
            }
        }
        // --------------------------------------------------------

        // 3. Enum Conversion & Status Logic
        RiskLevel prob = request.getProbability() != null
                ? RiskLevel.fromLabel(request.getProbability())
                : risk.getProbability();

        RiskLevel imp = request.getImpact() != null
                ? RiskLevel.fromLabel(request.getImpact())
                : risk.getImpact();

        boolean hasMitigation = request.getMitigation_plan() != null && !request.getMitigation_plan().trim().isEmpty();
        RiskStatus status = hasMitigation ? RiskStatus.RESOLVED : RiskStatus.UNRESOLVED;

        // 4. Update Core Fields
        risk.setRiskStatement(request.getRisk_statement());
        risk.setProbability(prob);
        risk.setImpact(imp);
        risk.setStatus(status);
        risk.setMitigationPlan(request.getMitigation_plan());
        risk.setNote(request.getNote());

        // 5. Update Revised Fields
        if (request.getRevised_probability() != null && !request.getRevised_probability().isEmpty()) {
            risk.setRevisedProbability(RiskLevel.fromLabel(request.getRevised_probability()));
        } else {
            risk.setRevisedProbability(null);
        }

        if (request.getRevised_impact() != null && !request.getRevised_impact().isEmpty()) {
            risk.setRevisedImpact(RiskLevel.fromLabel(request.getRevised_impact()));
        } else {
            risk.setRevisedImpact(null);
        }

        riskRepository.save(risk);

        // 6. Update Assignees (Delete Old -> Save New)
        if (request.getAssignees() != null) { // Check null to allow skipping updates if field is missing
            // A. ALWAYS Clear existing assignees first
            riskAssigneeRepository.deleteAllByRiskId(riskId);

            // B. Save new ones (We already validated them at the top!)
            if (!request.getAssignees().isEmpty()) {
                List<PM_RiskAssignee> newAssignees = request.getAssignees().stream()
                        .map(uid -> new PM_RiskAssignee(riskId, uid, risk))
                        .collect(Collectors.toList());
                riskAssigneeRepository.saveAll(newAssignees);
            }
        }

        // 7. Response
        Map<String, Object> data = new HashMap<>();
        data.put("riskId", risk.getRiskId());
        data.put("status", risk.getStatus().name());

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Risk updated", data);
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteRisk(Long userId, Long projectId, Long riskId) {
        // 1. Auth: OWNER ONLY
        authHelper.requireOwner(projectId, userId);

        // 2. Fetch Risk
        if (riskRepository.findById(riskId).isEmpty()) {
            return new BaseResponse<>(0, "Risk not found", null);
        }

        PM_Risk risk = riskRepository.findById(riskId)
                .orElseThrow(() -> new RuntimeException("Risk not found"));

        if (!risk.getProjectId().equals(projectId)) {
            throw new RuntimeException("Risk does not belong to this project");
        }

        riskRepository.delete(risk);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Risk deleted", Collections.emptyMap());
    }

    private String getRiskDegree(int score) {
        // Technically 13 and 14 are impossible, so >= 15 is safe
        if (score >= 15) return "HIGH";
        if (score >= 6) return "MEDIUM";
        return "LOW";
    }
}