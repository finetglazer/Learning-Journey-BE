package com.graduation.projectservice.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.graduation.projectservice.constant.FileConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileStorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${gcs.base-url}")
    private String baseUrl;

    public String uploadFile(Long projectId, MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        // Generate Object Name
        String objectName = String.format("projects/%d/%s.%s", projectId, UUID.randomUUID(), extension);
        BlobId blobId = BlobId.of(bucketName, objectName);

        // --- FIX: Force UTF-8 for text files ---
        String contentType = file.getContentType();
        if (contentType != null && (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml"))) {
            // If it is text/plain, change it to text/plain; charset=utf-8
            contentType += "; charset=utf-8";
        }
        // ---------------------------------------

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType) // Use the modified content type
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("%s/%s/%s", baseUrl, bucketName, objectName);
    }

    public void deleteFile(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) return;
        try {
            String prefix = String.format("%s/%s/", baseUrl, bucketName);
            if (!fullUrl.startsWith(prefix)) {
                log.warn("Invalid GCS URL format, cannot delete: {}", fullUrl);
                return;
            }
            String objectName = fullUrl.substring(prefix.length());

            BlobId blobId = BlobId.of(bucketName, objectName);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("Deleted GCS file: {}", objectName);
            } else {
                log.warn("GCS file not found for deletion: {}", objectName);
            }
        } catch (Exception e) {
            log.error("Error deleting file from GCS: {}", fullUrl, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > FileConstant.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        if (!FileConstant.ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            log.warn("Rejected content type: {}", file.getContentType());
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) return "bin";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}