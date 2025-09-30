package com.graduation.userservice.utils;

import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.payload.response.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

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
}
