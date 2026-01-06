package com.graduation.projectservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Custom GCS Configuration that reads credentials from environment variable.
 * This is needed for Docker deployment where ADC (Application Default
 * Credentials) is not available.
 * This overrides the default spring-cloud-gcp autoconfiguration.
 */
@Slf4j
@Configuration
public class GcsConfig {

    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.credentials.json:#{null}}")
    private String credentialsJson;

    @Bean
    @Primary
    public Storage storage() {
        try {
            GoogleCredentials credentials;

            if (credentialsJson != null && !credentialsJson.isEmpty()) {
                // Use credentials from environment variable (for Docker)
                log.info("Configuring GCS with credentials from environment variable");
                ByteArrayInputStream stream = new ByteArrayInputStream(
                        credentialsJson.getBytes(StandardCharsets.UTF_8));
                credentials = GoogleCredentials.fromStream(stream);
            } else {
                // Fall back to Application Default Credentials (for local dev)
                log.info("Configuring GCS with Application Default Credentials");
                credentials = GoogleCredentials.getApplicationDefault();
            }

            return StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (Exception e) {
            log.error("Failed to configure GCS Storage", e);
            throw new RuntimeException("Failed to configure GCS Storage: " + e.getMessage(), e);
        }
    }
}
