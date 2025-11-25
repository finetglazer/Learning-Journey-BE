package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliverableProgressDTO {
    private Long id;
    private String name;
    private String key;
    private Integer percentage;
}