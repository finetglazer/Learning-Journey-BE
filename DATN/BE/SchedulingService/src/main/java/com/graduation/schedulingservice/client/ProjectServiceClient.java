package com.graduation.schedulingservice.client;

import com.graduation.schedulingservice.payload.response.PM_TasKDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.project-service.url}")
    private String projectServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public Optional<PM_TasKDTO> getProjectTaskById(Long pmTaskId) {
        String url = projectServiceUrl + "/api/internal/tasks/" + pmTaskId;

        try {
            // Create headers and add the API key
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-API-Key", internalApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<PM_TasKDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PM_TasKDTO.class
            );

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No tasks found in Project Service");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch tasks: {}", e.getMessage());
            return Optional.empty();
        }
    }
}