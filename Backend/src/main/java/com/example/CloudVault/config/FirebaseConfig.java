package com.example.CloudVault.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

@Configuration

public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${gcp.credentials.location}")
    private String credentialsLocation;

    @Bean
    public FirebaseAuth firebaseAuth() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;
                try {
                    serviceAccount = new FileInputStream(credentialsLocation);
                } catch (IOException e) {
                    // If not found, try to load from classpath
                    serviceAccount = getClass().getResourceAsStream("/service-account-key.json");
                    if (serviceAccount == null) {
                        throw new IllegalStateException(
                                "Firebase service account not found in file system or classpath");
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://cloudvault-dd16d-default-rtdb.firebaseio.com") // Add this line
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
}
