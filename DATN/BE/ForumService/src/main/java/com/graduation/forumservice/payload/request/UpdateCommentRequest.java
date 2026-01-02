package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCommentRequest {
    @NotBlank(message = "Content cannot be empty")
    private String content;
}
