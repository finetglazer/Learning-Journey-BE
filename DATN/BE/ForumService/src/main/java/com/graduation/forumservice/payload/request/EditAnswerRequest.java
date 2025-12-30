package com.graduation.forumservice.payload.request;

import lombok.Data;

import java.util.Map;

@Data
public class EditAnswerRequest {
    private Map<String, Object> content;
}