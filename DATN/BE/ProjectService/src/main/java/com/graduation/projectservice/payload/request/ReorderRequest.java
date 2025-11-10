package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    @NotNull(message = "Reorder type is required")
    private ReorderType type;

    @NotNull(message = "Parent ID is required")
    private Long parentId;

    @NotEmpty(message = "Ordered IDs list cannot be empty")
    private List<Long> orderedIds;
}