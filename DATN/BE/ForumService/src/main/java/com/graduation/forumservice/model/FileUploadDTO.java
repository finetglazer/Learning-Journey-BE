package com.graduation.forumservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadDTO {
    private Long fileId;
    private String name;
    private String extension;
    private Long size;
    private String url;
}