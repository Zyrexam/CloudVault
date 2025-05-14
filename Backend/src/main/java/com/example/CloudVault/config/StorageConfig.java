package com.example.CloudVault.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class StorageConfig {

    @Bean
    public Storage storage() throws IOException {
        InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("service-account-key.json"); // use correct filename
        return StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(serviceAccount))
                .build()
                .getService();
    }
}
