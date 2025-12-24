package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Target type is required")
    @Pattern(regexp = "POST|ANSWER", message = "Target type must be POST or ANSWER")
    private String targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotBlank(message = "Comment content cannot be empty")
    private String content;

    private Long replyToCommentId; // Optional: for nested replies
}