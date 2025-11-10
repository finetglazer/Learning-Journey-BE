package com.graduation.projectservice.controller;

import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.payload.request.CreateDeliverableRequest;
import com.graduation.projectservice.payload.request.UpdateDeliverableRequest;
import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.service.DeliverableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/projects/{projectId}/deliverables")
@RequiredArgsConstructor
public class DeliverableController {

    private final DeliverableService deliverableService;

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createDeliverable(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateDeliverableRequest request) {

        log.info(Constant.LOG_POST_DELIVERABLE_REQUEST, projectId, userId);

        BaseResponse<?> response = deliverableService.createDeliverable(userId, projectId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{deliverableId}")
    public ResponseEntity<BaseResponse<?>> updateDeliverable(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody UpdateDeliverableRequest request) {

        log.info(Constant.LOG_PUT_DELIVERABLE_REQUEST, deliverableId, projectId, userId);

        BaseResponse<?> response = deliverableService.updateDeliverable(userId, projectId, deliverableId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{deliverableId}")
    public ResponseEntity<BaseResponse<?>> deleteDeliverable(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long deliverableId) {

        log.info(Constant.LOG_DELETE_DELIVERABLE_REQUEST, deliverableId, projectId, userId);

        BaseResponse<?> response = deliverableService.deleteDeliverable(userId, projectId, deliverableId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/structure")
    public ResponseEntity<BaseResponse<?>> getProjectStructure(
            @PathVariable Long projectId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info(Constant.LOG_GET_PROJECT_STRUCTURE_REQUEST, projectId, userId);


        return ResponseEntity.ok(new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.PROJECT_STRUCTURE_RETRIEVED_SUCCESS,
                deliverableService.getProjectStructure(projectId, userId)
        ));
    }
}