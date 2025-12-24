package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class CreateAnswerRequest {
    @NotEmpty(message = "Answer content cannot be empty")
    private Map<String, Object> content; // Notion-style JSON
}