package com.graduation.notificationservice.client;

import com.graduation.notificationservice.payload.response.UserBatchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

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
     * Find user by ID
     * 
     * @param userId User ID to fetch
     * @return Optional of UserBatchDTO
     */
    public Optional<UserBatchDTO> findById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String url = userServiceUrl + "/api/internal/users/" + userId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserBatchDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserBatchDTO.class);

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found with ID: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch user by ID {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
