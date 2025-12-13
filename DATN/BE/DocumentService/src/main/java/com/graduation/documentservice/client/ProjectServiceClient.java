package com.graduation.documentservice.client;

import com.graduation.documentservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.project-service.url}") // Make sure this is in application.properties!
    private String projectServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    /**
     * Sync a newly created MongoDB snapshot to the SQL Project Service
     */
    public boolean syncSnapshotToVersion(Long pgNodeId, String snapshotRef, Long userId, String reason) {
        // Internal API endpoint we are about to create in Project Service
        String url = projectServiceUrl + "/api/internal/pm/files/" + pgNodeId + "/version/sync";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("snapshotRef", snapshotRef); // MongoDB String ID
            requestBody.put("userId", userId);
            requestBody.put("reason", reason);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BaseResponse<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<Void>>() {}
            );

            if (response.getBody() != null && response.getBody().getStatus() == 1) {
                log.info("Successfully synced snapshot {} to project service for node {}", snapshotRef, pgNodeId);
                return true;
            }

            log.error("Failed to sync snapshot: {}", response.getBody() != null ? response.getBody().getMsg() : "No response");
            return false;

        } catch (Exception e) {
            log.error("Error syncing snapshot {} to project service: {}", snapshotRef, e.getMessage());
            return false;
        }
    }
}