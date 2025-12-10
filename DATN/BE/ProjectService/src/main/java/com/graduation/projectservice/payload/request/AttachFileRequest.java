package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachFileRequest {

    @NotNull(message = "Node ID is required")
    private Long nodeId;
}
