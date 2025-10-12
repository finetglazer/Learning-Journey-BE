package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isComplete;
    private LocalDateTime completedAt;
}