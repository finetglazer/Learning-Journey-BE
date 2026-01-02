package com.graduation.forumservice.client;

import com.graduation.forumservice.payload.response.UserBatchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestTemplate restTemplate;

    @Value("${app.services.user-service.url}") // Make sure this is in application.properties!
    private String userServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    /**
     * Fetches user metadata (name, avatar) from the User Service via Internal API.
     * Mimics the code style of ProjectServiceClient.
     */
    public Optional<UserBatchDTO> getUserById(Long userId) {
        // Internal API endpoint in User Service
        String url = userServiceUrl + "/api/internal/users/" + userId;

        try {
            // Setup security headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Execute GET request
            ResponseEntity<UserBatchDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserBatchDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully fetched user info for ID: {}", userId);
                return Optional.of(response.getBody());
            }

            log.warn("User Service returned status {} for user ID: {}", response.getStatusCode(), userId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error calling User Service for ID {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
