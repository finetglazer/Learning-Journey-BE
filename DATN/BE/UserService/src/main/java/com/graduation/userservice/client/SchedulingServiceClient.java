package com.graduation.userservice.client;

import com.graduation.userservice.payload.request.TimezoneConversionRequest;
import com.graduation.userservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingServiceClient {

    private final RestTemplate restTemplate;

    @Value("${scheduling.service.url:http://localhost:8082}")
    private String schedulingServiceUrl;

    @Value("${app.security.internal-api-key}") // ADD THIS
    private String internalApiKey;

    public boolean convertUserTimezone(Long userId, String oldTimezone, String newTimezone) {
        try {
            String url = schedulingServiceUrl + "/api/internal/users/" + userId + "/timezone";

            TimezoneConversionRequest request = new TimezoneConversionRequest(oldTimezone, newTimezone);

            // ADD API KEY HEADER
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            log.info("Calling SchedulingService to convert timezone for user {}: {} -> {}",
                    userId, oldTimezone, newTimezone);

            ResponseEntity<BaseResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers), // ADD headers here
                    BaseResponse.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getStatus() == 1;

            if (success) {
                log.info("Successfully converted calendar items for user {}", userId);
            } else {
                log.warn("Failed to convert calendar items for user {}: {}",
                        userId, response.getBody());
            }

            return success;

        } catch (Exception e) {
            log.error("Error calling SchedulingService to convert timezone for user {}", userId, e);
            return false;
        }
    }

    public void createDefaultCalendar(Long userId) {
        try {
            String url = schedulingServiceUrl + "/api/internal/users/" + userId + "/default-calendar";

            // ADD API KEY HEADER
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            log.info("Calling SchedulingService to create default calendar for user {}", userId);

            ResponseEntity<BaseResponse> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(null, headers), // ADD headers here
                    BaseResponse.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getStatus() == 1;

            if (success) {
                log.info("Successfully created default calendar for user {}", userId);
            } else {
                log.warn("Failed to create default calendar for user {}: {}",
                        userId, response.getBody());
            }

        } catch (Exception e) {
            log.error("Error calling SchedulingService to create default calendar for user {}", userId, e);
        }
    }
}