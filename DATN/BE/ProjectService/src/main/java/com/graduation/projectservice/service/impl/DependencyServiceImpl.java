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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        // 2. Fetch Dependencies
        List<PM_Dependency> dependencies = dependencyRepository.findAllRelatedDependencies(projectId, itemType, itemId);

        // 3. Validation: Check if Items Exist (Returns 0 and null data if not found)
        if (!isItemExist(itemId, itemType)) {
            return new BaseResponse<>(0, itemType + " not found with id: " + itemId, null);
        }


        // 4. Map to DTO
        List<DependencyDTO> dtos = dependencies.stream()
                .map(d -> DependencyDTO.builder()
                        .type(d.getType())
                        .fromId(d.getSourceId())
                        .toId(d.getTargetId())
                        .build())
                .collect(Collectors.toList());

        return new BaseResponse<>(1, "Dependencies for " + itemType + "-" + itemId + " retrieved",
                Map.of("dependencies", dtos));
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