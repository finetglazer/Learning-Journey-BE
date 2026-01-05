package com.graduation.projectservice.client;

import com.graduation.projectservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.forum-service.url}")
    private String forumServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    /**
     * Internal: Fetch all forum posts that have been shared/saved to a specific project.
     * Called by ProjectService to populate the "Shared Resources" view.
     */
    public BaseResponse<List<Map<String, Object>>> getPostsByProject(Long projectId) {
        // Construct the internal endpoint URL
        String url = forumServiceUrl + "/api/internal/forum/projects/" + projectId;

        try {
            log.debug("Internal Call: Fetching shared posts for project {} from Forum Service", projectId);

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Use ParameterizedTypeReference to handle the generic List inside BaseResponse
            ResponseEntity<BaseResponse<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<BaseResponse<List<Map<String, Object>>>>() {}
            );

            if (response.getBody() != null) {
                return response.getBody();
            }

            return new BaseResponse<>(0, "Empty response from Forum Service", null);

        } catch (Exception e) {
            log.error("Error calling Forum Service for project {}: {}", projectId, e.getMessage());
            return new BaseResponse<>(0, "Internal Communication Error: " + e.getMessage(), null);
        }
    }

    /**
     * Helper to create standard internal security headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-API-Key", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}