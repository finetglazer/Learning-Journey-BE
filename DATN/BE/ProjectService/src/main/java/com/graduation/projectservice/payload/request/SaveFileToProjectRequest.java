package com.graduation.projectservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for copying a forum attachment into a Project Manager space.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveFileToProjectRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "File ID is required")
    private Long fileId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    // This can be null if saving to the project root
    private Long folderId;

    @NotNull(message = "File size is required")
    private Long fileSize;

    @NotNull(message = "Storage reference is required")
    private String storageRef;

    @NotNull(message = "File name is required")
    private String name;

    private String extension;
}