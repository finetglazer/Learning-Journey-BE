package com.graduation.userservice.payload.request;

import com.graduation.userservice.constant.Constant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = Constant.VALIDATION_EMAIL_REQUIRED)
    @Email(message = Constant.VALIDATION_EMAIL_FORMAT)
    private String email;
}