package com.graduation.schedulingservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorableEventResponseDTO {

    private Long id;
    private String title;
    private Integer day;
    private Integer month;
}