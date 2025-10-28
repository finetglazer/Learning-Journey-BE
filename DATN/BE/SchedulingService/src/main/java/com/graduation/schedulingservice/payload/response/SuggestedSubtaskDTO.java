package com.graduation.schedulingservice.payload.response;

import lombok.Data;

@Data
public class SuggestedSubtaskDTO {
    private Long id;
    private String name;
    private String description;
    private String estimated;
}