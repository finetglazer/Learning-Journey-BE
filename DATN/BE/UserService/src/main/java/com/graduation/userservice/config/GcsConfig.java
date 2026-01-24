package com.graduation.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class GcsConfig {

    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.credentials.json}")
    private String credentialsJson;

    @Bean
    public Storage storage() throws IOException {
        // 1. Clean the input (remove potential accidental quotes added by env parsers)
        String cleanedCredentials = credentialsJson.replace("'", "").trim();

        byte[] credentialBytes;

        // 2. Smart Detection: Is it JSON or Base64?
        // JSON always starts with '{'. Base64 usually starts with alphanumeric chars.
        if (cleanedCredentials.startsWith("{")) {
            // It is raw JSON (Local environment)
            credentialBytes = cleanedCredentials.getBytes();
        } else {
            // It is Base64 encoded (Cloud environment)
            // This fixes the "MalformedJsonException" you are seeing!
            try {
                credentialBytes = Base64.getDecoder().decode(cleanedCredentials);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Failed to decode GCS credentials. Check if GCS_CREDENTIALS_JSON is valid JSON or Base64.", e);
            }
        }

        // 3. Create Credentials from the bytes (whether they were raw or decoded)
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialBytes)
        );

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}