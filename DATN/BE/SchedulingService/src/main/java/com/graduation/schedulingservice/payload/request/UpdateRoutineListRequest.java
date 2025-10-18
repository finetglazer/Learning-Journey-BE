package com.graduation.schedulingservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoutineListRequest {

    @NotNull(message = "Approved routine names list is required")
    private List<String> approvedRoutineNames;
}
