package com.graduation.userservice.config;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
public class GcsConfig {

    // 1. Inject the properties from application.properties
    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.credentials.json}")
    private String credentialsJson;

    /**
     * This @Bean creates the central Storage object that
     * your entire application will use to interact with GCS.
     */
    @Bean
    public Storage storage() throws IOException {

        Credentials credentials;

        if (StringUtils.hasText(credentialsJson)) {
            // Use the JSON credentials from the environment variable
            credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson.getBytes())
            );
        } else {
            // Fallback: Use "Application Default Credentials"
            // This is good for production (e.g., Cloud Run, GKE)
            credentials = GoogleCredentials.getApplicationDefault();
        }

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}