package com.graduation.schedulingservice.payload.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemorableEventsRequest {

    @NotNull(message = "Events list is required")
    @Valid
    private List<MemorableEventDTO> events;
}