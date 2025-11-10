package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreatePhaseRequest;
import com.graduation.projectservice.payload.request.UpdatePhaseRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface PhaseService {

    BaseResponse<?> createPhase(Long userId, Long projectId, Long deliverableId, CreatePhaseRequest request);

    BaseResponse<?> updatePhase(Long userId, Long projectId, Long phaseId, UpdatePhaseRequest request);

    BaseResponse<?> deletePhase(Long userId, Long projectId, Long phaseId);
}