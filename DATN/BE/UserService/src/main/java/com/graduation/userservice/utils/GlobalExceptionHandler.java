package com.graduation.userservice.utils;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.payload.response.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK) // Always return 200 OK
    public ResponseEntity<BaseResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ValidationErrorResponse> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return new ValidationErrorResponse(fieldName, errorMessage);
                })
                .collect(Collectors.toList());

        BaseResponse<List<ValidationErrorResponse>> response = new BaseResponse<>(
                0,
                Constant.MSG_VALIDATION_FAILED,
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<BaseResponse<?>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn(Constant.LOG_AVATAR_FILE_TOO_LARGE, "N/A", e.getMessage());

        // Create the error response you want
        BaseResponse<?> errorResponse = new BaseResponse<>(0, Constant.MSG_FILE_TOO_LARGE, null);

        // Return it with a 400 Bad Request status
        // This is more semantically correct than a 200 OK for a bad file.
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
