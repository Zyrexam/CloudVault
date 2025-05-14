package com.example.CloudVault.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class GCSService {

    private final Storage storage;

    @Value("${firebase.bucket-name}")
    private String bucketName;

    public GCSService(Storage storage) {
        this.storage = storage;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, file.getOriginalFilename()).build();
        storage.create(blobInfo, file.getBytes());
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, file.getOriginalFilename());
    }
}
