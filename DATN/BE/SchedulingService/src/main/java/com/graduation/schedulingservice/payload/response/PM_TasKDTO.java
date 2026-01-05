package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PM_TasKDTO {
    private Long id;
    private Long phaseId;
    private String name;
    private String key;
    private String status;
    private String priority;
    private Integer order;
    private LocalDate startDate;
    private LocalDate endDate;
}
