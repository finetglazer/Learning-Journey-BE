package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_Dependency;
import com.graduation.projectservice.payload.request.DependencyRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.DependencyDTO;
import com.graduation.projectservice.repository.DeliverableRepository;
import com.graduation.projectservice.repository.DependencyRepository;
import com.graduation.projectservice.repository.PhaseRepository;
import com.graduation.projectservice.repository.TaskRepository;
import com.graduation.projectservice.service.DependencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyServiceImpl implements DependencyService {

    private final ProjectAuthorizationHelper authHelper;
    private final DependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final DeliverableRepository deliverableRepository;


    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getDependencies(Long userId, Long projectId, Long itemId, String itemType) {
        // 1. Auth Check
        authHelper.requireActiveMember(projectId, userId);

        // 2. Validation: Check if Items Exist
        if (!isItemExist(itemId, itemType)) {
            return new BaseResponse<>(0, itemType + " not found with id: " + itemId, null);
        }

        // 3. Fetch ALL dependencies for this project/type to build the graph
        // (It is much faster to fetch 500 rows once than to run 500 individual queries)
        List<PM_Dependency> allProjectDependencies = dependencyRepository
                .findAllByProjectIdAndType(projectId, itemType);

        // 4. BFS Algorithm to find connected components
        Set<PM_Dependency> relatedDependencies = performBFS(allProjectDependencies, itemId);

        // 5. Map to DTO
        List<DependencyDTO> dtos = relatedDependencies.stream()
                .map(d -> DependencyDTO.builder()
                        .type(d.getType())
                        .fromId(d.getSourceId())
                        .toId(d.getTargetId())
                        .build())
                .collect(Collectors.toList());

        return new BaseResponse<>(1,
                "All recursive dependencies for " + itemType + "-" + itemId + " retrieved",
                Map.of("dependencies", dtos));
    }

    /**
     * Performs Breadth-First Search to find all dependencies connected to the startNode.
     * It traverses both Upstream (Predecessors) and Downstream (Successors).
     */
    private Set<PM_Dependency> performBFS(List<PM_Dependency> allDependencies, Long startNodeId) {
        Set<PM_Dependency> result = new HashSet<>();
        Set<Long> visitedNodes = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();

        // Initialize
        queue.add(startNodeId);
        visitedNodes.add(startNodeId);

        while (!queue.isEmpty()) {
            Long currentNode = queue.poll();

            // Find all connections involving the current node
            for (PM_Dependency dep : allDependencies) {

                // Check if we have already added this specific dependency link to results
                if (result.contains(dep)) continue;

                boolean isRelated = false;
                Long nextNode = null;

                // Case A: Downstream (Current -> Target)
                if (dep.getSourceId().equals(currentNode)) {
                    nextNode = dep.getTargetId();
                    isRelated = true;
                }
                // Case B: Upstream (Source -> Current)
                else if (dep.getTargetId().equals(currentNode)) {
                    nextNode = dep.getSourceId();
                    isRelated = true;
                }

                if (isRelated) {
                    result.add(dep); // Add the link to our result set

                    // If we haven't visited the connected node, add it to queue
                    if (!visitedNodes.contains(nextNode)) {
                        visitedNodes.add(nextNode);
                        queue.add(nextNode);
                    }
                }
            }
        }

        return result;
    }

    @Override
    @Transactional
    public BaseResponse<?> createDependency(Long userId, Long projectId, DependencyRequest request) {
        // 1. Auth Check
        authHelper.isOwner(projectId, userId);

        // 2. Validation: Self-Reference
        if (request.getFromId().equals(request.getToId())) {
            // Depending on preference, you can also return status 0 here instead of throwing
            return new BaseResponse<>(0, "Cannot create dependency to self", null);
        }

        // 3. Validation: Check if Items Exist (Returns 0 and null data if not found)
        if (!isItemExist(request.getFromId(), request.getType())) {
            return new BaseResponse<>(0, request.getType() + " not found with id: " + request.getFromId(), null);
        }

        if (!isItemExist(request.getToId(), request.getType())) {
            return new BaseResponse<>(0, request.getType() + " not found with id: " + request.getToId(), null);
        }

        // 4. Check if dependency already exists
        boolean exists = dependencyRepository.findByProjectIdAndTypeAndSourceIdAndTargetId(
                projectId, request.getType(), request.getFromId(), request.getToId()
        ).isPresent();

        if (exists) {
            return new BaseResponse<>(0, "Dependency already exists", null);
        }

        // 5. Save new dependency
        PM_Dependency dependency = new PM_Dependency();
        dependency.setProjectId(projectId);
        dependency.setType(request.getType());
        dependency.setSourceId(request.getFromId());
        dependency.setTargetId(request.getToId());

        dependencyRepository.save(dependency);

        return new BaseResponse<>(1, "Dependency created", Collections.emptyMap());
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteDependency(Long userId, Long projectId, DependencyRequest request) {
        // 1. Auth Check
        authHelper.isOwner(projectId, userId);

        // 2. Validation: Check if Items Exist (Returns 0 and null data if not found)
        if (!isItemExist(request.getFromId(), request.getType())) {
            return new BaseResponse<>(0, request.getType() + " not found with id: " + request.getFromId(), null);
        }

        if (!isItemExist(request.getToId(), request.getType())) {
            return new BaseResponse<>(0, request.getType() + " not found with id: " + request.getToId(), null);
        }

        // 3. Find and Delete
        dependencyRepository.findByProjectIdAndTypeAndSourceIdAndTargetId(
                projectId, request.getType(), request.getFromId(), request.getToId()
        ).ifPresent(dependencyRepository::delete);

        return new BaseResponse<>(1, "Dependency deleted", Collections.emptyMap());
    }

    // Helper method changed to return boolean instead of throwing Exception
    private boolean isItemExist(Long id, String type) {
        return switch (type) {
            case "TASK" -> taskRepository.existsById(id);
            case "PHASE" -> phaseRepository.existsById(id);
            case "DELIVERABLE" -> deliverableRepository.existsById(id);
            default -> false;
        };
    }
}