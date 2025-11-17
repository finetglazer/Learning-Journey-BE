package com.graduation.projectservice.client;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.UserBatchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.user-service.url}")
    private String userServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    /**
     * Find user by email
     */
    public Optional<UserBatchDTO> findUserByEmail(String email) {
        String url = userServiceUrl + "/api/internal/users/by-email/" + email;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UserBatchDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserBatchDTO.class
            );

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found with email: {}", email);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch user by email {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find users by email
     */
    public List<UserBatchDTO> findUsersByEmail(String email) {
        String url = userServiceUrl + "/api/internal/users/find-users-by-email/" + email;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ParameterizedTypeReference<List<UserBatchDTO>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<List<UserBatchDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType,
                    email
            );

            List<UserBatchDTO> users = response.getBody();
            return users != null ? users : Collections.emptyList();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found with email: {}", email);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch user by email {}: {}", email, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Batch fetch users by IDs
     */
    public List<UserBatchDTO> findUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        String url = userServiceUrl + "/api/internal/users/batch-by-ids";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Long>> entity = new HttpEntity<>(userIds, headers);

            ResponseEntity<List<UserBatchDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<UserBatchDTO>>() {
                    }
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to batch fetch users: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Send invitation email and create token
     */
    public Optional<String> sendInvitation(Long userId, Long projectId, String projectName) {
        String url = userServiceUrl + "/api/internal/users/send-invitation";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("projectId", projectId);
            requestBody.put("projectName", projectName);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, String>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().containsKey("token")) {
                return Optional.of(response.getBody().get("token"));
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to send invitation for user {} to project {}: {}", userId, projectId, e.getMessage());
            return Optional.empty();
        }
    }


    public Map<String, Long> validateInvitationToken(String token) {
        try {
            String url = userServiceUrl + "/api/internal/users/validate-invitation-token?token=" + token;

            // Create headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Use exchange instead of postForEntity to include headers
            ResponseEntity<BaseResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    BaseResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    response.getBody().getStatus() == 1) {

                Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
                return Map.of(
                        "userId", ((Number) data.get("userId")).longValue(),
                        "projectId", ((Number) data.get("projectId")).longValue()
                );
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to validate invitation token: {}", e.getMessage(), e);
            return null;
        }
    }

}
