package com.graduation.schedulingservice.payload.response;

import lombok.Data;

@Data
public class SuggestedSubtaskDTO {
    private String name;
    private String description;
    private String estimated;
}