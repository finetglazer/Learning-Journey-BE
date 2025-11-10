package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.CreatePhaseRequest;
import com.graduation.projectservice.payload.request.UpdatePhaseRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.PhaseTasksResponse;
import com.graduation.projectservice.service.PhaseService;
import com.graduation.projectservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;
    private final TaskService taskService;

    @PostMapping("/deliverables/{deliverableId}/phases")
    public ResponseEntity<BaseResponse<?>> createPhase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody CreatePhaseRequest request) {

        log.info(Constant.LOG_POST_PHASE_REQUEST, deliverableId, projectId, userId);

        BaseResponse<?> response = phaseService.createPhase(userId, projectId, deliverableId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/phases/{phaseId}")
    public ResponseEntity<BaseResponse<?>> updatePhase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @Valid @RequestBody UpdatePhaseRequest request) {

        log.info(Constant.LOG_PUT_PHASE_REQUEST, phaseId, projectId, userId);

        BaseResponse<?> response = phaseService.updatePhase(userId, projectId, phaseId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/phases/{phaseId}")
    public ResponseEntity<BaseResponse<?>> deletePhase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long phaseId) {

        log.info(Constant.LOG_DELETE_PHASE_REQUEST, phaseId, projectId, userId);

        BaseResponse<?> response = phaseService.deletePhase(userId, projectId, phaseId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/phases/{phaseId}/tasks")
    public ResponseEntity<BaseResponse<?>> getPhaseTasksForList(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info(Constant.LOG_GET_PHASE_TASKS_REQUEST, projectId, phaseId, userId);


        return ResponseEntity.ok(new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                String.format(Constant.PHASE_TASKS_RETRIEVED_SUCCESS, phaseId),
                taskService.getTasksByPhase(projectId, phaseId, userId)
        ));
    }
}