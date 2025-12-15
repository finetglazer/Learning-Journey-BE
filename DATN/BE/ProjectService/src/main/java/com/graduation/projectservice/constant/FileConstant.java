package com.graduation.projectservice.constant;

import java.util.Arrays;
import java.util.List;

public class FileConstant {
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // Mime types based on your image: ZIP, RAR, TXT, PPT, PDF, JPG, DOC, PNG, XLS
    public static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/zip", "application/x-zip-compressed",
            "application/x-rar-compressed", "application/vnd.rar",
            "text/plain",
            "application/pdf",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PPT/PPTX
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOC/DOCX
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLS/XLSX
            "image/jpeg", "image/png", "image/jpg"
    );

    // Image types for editor (more permissive)
    public static final List<String> IMAGE_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/gif",
            "image/webp"
    );
}