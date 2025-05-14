package com.example.CloudVault.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Configuration
public class GCPStorageConfig {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Bean
    public Storage storage() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ClassPathResource("service-account-key.json").getInputStream()
        );
        return StorageOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(credentials)
            .build()
            .getService();
    }
} 