package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 1000, message = "Title cannot exceed 1000 characters")
    private String title;

    @NotEmpty(message = "Post content cannot be empty")
    private Map<String, Object> content; // Rich text JSON blocks

    private List<String> tags; // Backend creates tag if not exists
}