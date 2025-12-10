package com.graduation.projectservice.client;

import com.graduation.projectservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.document-service.url}")
    private String documentServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    /**
     * Create a new empty document in MongoDB
     * Returns the storage reference (MongoDB ObjectId)
     */
    public Optional<String> createDocument(Long pgNodeId, Long projectId) {
        String url = documentServiceUrl + "/api/internal/documents";

        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("pgNodeId", pgNodeId);
            requestBody.put("projectId", projectId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BaseResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().getStatus() == 1) {
                Map<String, Object> data = response.getBody().getData();
                if (data != null && data.containsKey("storageReference")) {
                    return Optional.of(data.get("storageReference").toString());
                }
            }

            log.error("Failed to create document: {}", response.getBody() != null ? response.getBody().getMsg() : "No response");
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error creating document for pgNodeId {}: {}", pgNodeId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Load document content from MongoDB
     */
    public Optional<Map<String, Object>> loadDocument(String storageRef) {
        String url = documentServiceUrl + "/api/internal/documents/" + storageRef;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BaseResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().getStatus() == 1) {
                return Optional.ofNullable(response.getBody().getData());
            }

            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Document not found: {}", storageRef);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error loading document {}: {}", storageRef, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete document and all its snapshots from MongoDB
     */
    public boolean deleteDocument(String storageRef) {
        String url = documentServiceUrl + "/api/internal/documents/" + storageRef;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BaseResponse<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Void>>() {}
            );

            return response.getBody() != null && response.getBody().getStatus() == 1;

        } catch (Exception e) {
            log.error("Error deleting document {}: {}", storageRef, e.getMessage());
            return false;
        }
    }

    /**
     * Create a version snapshot
     */
    public Optional<Map<String, Object>> createSnapshot(String storageRef, String reason, Long createdBy) {
        String url = documentServiceUrl + "/api/internal/documents/" + storageRef + "/snapshot";

        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("createdBy", createdBy);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BaseResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().getStatus() == 1) {
                return Optional.ofNullable(response.getBody().getData());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error creating snapshot for {}: {}", storageRef, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get snapshot content
     */
    public Optional<Map<String, Object>> getSnapshot(String storageRef, String snapshotId) {
        String url = documentServiceUrl + "/api/internal/documents/" + storageRef + "/snapshot/" + snapshotId;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BaseResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().getStatus() == 1) {
                return Optional.ofNullable(response.getBody().getData());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting snapshot {} for {}: {}", snapshotId, storageRef, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-API-Key", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
