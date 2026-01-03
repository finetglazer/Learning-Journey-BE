package com.graduation.forumservice.client;

import com.graduation.forumservice.model.ForumPostFile;
import com.graduation.forumservice.payload.request.SaveFileToProjectRequest;
import com.graduation.forumservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceClient {
    private final RestTemplate restTemplate;

    @Value("${app.services.project-service.url}")
    private String projectServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public BaseResponse<?> uploadMultipleFiles(Long userId, Long projectId, List<MultipartFile> files) {
        String url = String.format("%s/api/pm/internal/files/%d/upload-multiple", projectServiceUrl, projectId);

        // 1. Prepare Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-User-Id", userId.toString());
        headers.set("X-Internal-Api-Key", internalApiKey);

        // 2. Prepare Body (MultiValueMap is required for multipart)
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            for (MultipartFile file : files) {
                // Convert MultipartFile to Resource so RestTemplate can stream it
                ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add("files", resource);
            }
        } catch (IOException e) {
            log.error("Failed to process files for internal upload: {}", e.getMessage());
            return new BaseResponse<>(0, "File processing error", null);
        }

        // 3. Execute Request
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Calling ProjectService internal upload for user {} and project {}", userId, projectId);
            return restTemplate.postForObject(url, requestEntity, BaseResponse.class);
        } catch (Exception e) {
            log.error("Internal call to ProjectService failed: {}", e.getMessage());
            return new BaseResponse<>(0, "Internal service communication error", null);
        }
    }

    public BaseResponse<?> deleteMultipleFiles(Long userId, List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            log.debug("No files provided for internal deletion cleanup.");
            return new BaseResponse<>(1, "No files to delete", null);
        }

        // Target the internal delete-multiple endpoint
        String url = String.format("%s/api/pm/internal/files/delete-multiple", projectServiceUrl);

        // 1. Prepare Security and Identity Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId.toString());
        headers.set("X-Internal-Api-Key", internalApiKey);

        // 2. Prepare Request Entity with the list of URLs in the body
        HttpEntity<List<String>> requestEntity = new HttpEntity<>(fileUrls, headers);

        try {
            log.info("Calling ProjectService internal cleanup for user {} ({} files)", userId, fileUrls.size());

            // Execute the POST request to trigger GCS cleanup
            return restTemplate.postForObject(url, requestEntity, BaseResponse.class);

        } catch (Exception e) {
            log.error("Internal call to ProjectService cleanup failed: {}", e.getMessage());
            // We return an error response, though typically this cleanup is best-effort
            return new BaseResponse<>(0, "Internal service cleanup communication error", null);
        }
    }

    /**
     * Calls ProjectService to link an existing storage object to a project tree.
     * Uses JSON metadata instead of raw file bytes for efficiency.
     */
    public BaseResponse<?> saveFileToProject(SaveFileToProjectRequest request) {
        // 1. Prepare the internal URL
        // The endpoint was refactored to a generic save-to-project JSON handler
        String url = String.format("%s/api/pm/internal/files/save-to-project", projectServiceUrl);

        // 2. Prepare Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", request.getUserId().toString());
        headers.set("X-Internal-Api-Key", internalApiKey);

        // 3. Wrap the request DTO in an HttpEntity
        HttpEntity<SaveFileToProjectRequest> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.info("Sending metadata link request: File {} -> Project {}",
                    request.getFileId(), request.getProjectId());

            // 4. Execute the POST request
            return restTemplate.postForObject(url, requestEntity, BaseResponse.class);

        } catch (Exception e) {
            log.error("Internal call to saveFileToProject (metadata) failed: {}", e.getMessage());
            return new BaseResponse<>(0, "Internal service communication error", null);
        }
    }
}
