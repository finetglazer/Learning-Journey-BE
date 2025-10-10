package com.graduation.userservice.client;

import com.graduation.userservice.payload.request.TimezoneConversionRequest;
import com.graduation.userservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
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

    /**
     * Request SchedulingService to convert all calendar items for a user
     * @param userId User ID
     * @param oldTimezone Previous timezone
     * @param newTimezone New timezone
     * @return true if conversion succeeded, false otherwise
     */
    public boolean convertUserTimezone(Long userId, String oldTimezone, String newTimezone) {
        try {
            String url = schedulingServiceUrl + "/api/internal/users/" + userId + "/timezone";

            TimezoneConversionRequest request = new TimezoneConversionRequest(oldTimezone, newTimezone);

            log.info("Calling SchedulingService to convert timezone for user {}: {} -> {}",
                    userId, oldTimezone, newTimezone);

            ResponseEntity<BaseResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
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
}