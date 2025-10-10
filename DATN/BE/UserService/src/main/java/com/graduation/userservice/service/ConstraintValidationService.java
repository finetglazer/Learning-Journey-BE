package com.graduation.userservice.service;

//import com.graduation.userservice.payload.request.ValidateConstraintsRequest;
import com.graduation.userservice.payload.response.BaseResponse;

public interface ConstraintValidationService {

    /**
     * Validate if a time slot violates any user constraints
     * @param userId The user ID
     * @param request Contains timeSlot and itemType
     * @return BaseResponse with violations list if any
     */
//    BaseResponse<?> validateTimeSlot(Long userId, ValidateConstraintsRequest request);
}