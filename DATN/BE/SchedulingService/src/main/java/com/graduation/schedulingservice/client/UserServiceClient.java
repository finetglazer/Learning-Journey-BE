package com.graduation.schedulingservice.client;

import com.graduation.schedulingservice.payload.response.UserConstraintsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.user-service.url}")
    private String userServiceUrl;

    @Value("${app.security.internal-api-key}") // ADD THIS
    private String internalApiKey;

    public Optional<UserConstraintsDTO> fetchUserConstraints(Long userId) {
        String url = userServiceUrl + "/api/internal/users/" + userId + "/constraints";

        try {
            // Create headers and add the API key
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey); // ADD THIS

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UserConstraintsDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserConstraintsDTO.class
            );

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No constraints found in User Service for user {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch constraints for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}