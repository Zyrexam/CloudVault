package com.example.CloudVault.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${gcp.credentials.location}")
    private String credentialsLocation;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bucket-name}")
    private String bucketName;


    @Bean
    public FirebaseAuth firebaseAuth() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getServiceAccountStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .setStorageBucket(bucketName)
                        .setDatabaseUrl("https://cloudvault-dd16d-default-rtdb.firebaseio.com")
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialization successful");
            }

            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            logger.error("Firebase initialization failed", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }


    @Bean
    public Storage storage() {
        try {
            InputStream serviceAccount = getServiceAccountStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build()
                    .getService();

            logger.info("Cloud Storage client initialized successfully for project: {}", projectId);
            return storage;
        } catch (Exception e) {
            logger.error("Failed to initialize Cloud Storage client", e);
            throw new RuntimeException("Failed to initialize Cloud Storage client", e);
        }
    }


    private InputStream getServiceAccountStream() throws IOException {
        try {
            // First try to load from file system
            return new FileInputStream(credentialsLocation);
        } catch (IOException e) {
            logger.warn("Could not load credentials from file: {}, trying classpath", credentialsLocation);
            // If not found, try to load from classpath
            InputStream serviceAccount = getClass().getResourceAsStream("/service-account-key.json");
            if (serviceAccount == null) {
                throw new IllegalStateException(
                        "Firebase service account not found in file system or classpath. " +
                                "Please check your gcp.credentials.location property or place service-account-key.json in resources folder");
            }
            return serviceAccount;
        }
    }
}