package com.graduation.projectservice.exception;

import com.graduation.projectservice.payload.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new BaseResponse<>(0, "Validation failed", errors);
    }

    // Handle Logic Errors (Forbidden)
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ForbiddenException.class)
    public BaseResponse<?> handleForbidden(ForbiddenException ex) {
        return new BaseResponse<>(0, ex.getMessage(), null);
    }

    // Handle Not Found
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(NotFoundException.class)
    public BaseResponse<?> handleNotFound(NotFoundException ex) {
        return new BaseResponse<>(0, ex.getMessage(), null);
    }

    // Handle File Size Limit
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<?> handleMaxSize(MaxUploadSizeExceededException ex) {
        return new BaseResponse<>(0, "File is too large. Maximum size is 10MB.", null);
    }

    // --- UPDATED: General Exception Handler ---
    // This catches "Unexpected" errors (like SQL crashes, NullPointers)
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleGeneral(Exception ex) {
        // 1. Log the REAL error to the console (for YOU to see)
        log.error("Internal Server Error: ", ex);

        // 2. Return a GENERIC friendly message to the Frontend (for User to see)
        // We do NOT send ex.getMessage() here because it might contain sensitive SQL info.
        return new BaseResponse<>(0, "An internal server error occurred. Please contact the administrator.", null);
    }

    // --- NEW: Handle JSON Parsing Errors (e.g., comments in JSON) ---
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleJsonErrors(HttpMessageNotReadableException ex) {
        log.error("JSON Parse Error: ", ex);
        return new BaseResponse<>(0, "Invalid JSON format in request body", null);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> handleIllegalArgument(IllegalArgumentException ex) {
        // Log as WARN because this is usually a user input error (not a system crash)
        log.warn("Input Validation Error: {}", ex.getMessage());

        // Return the actual message (e.g., "Unsupported file type: application/octet-stream")
        return new BaseResponse<>(0, ex.getMessage(), null);
    }
}