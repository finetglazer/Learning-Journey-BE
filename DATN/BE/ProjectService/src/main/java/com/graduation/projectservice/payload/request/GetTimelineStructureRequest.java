package com.graduation.projectservice.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetTimelineStructureRequest {
    private String search;
    private boolean showMine;
}
