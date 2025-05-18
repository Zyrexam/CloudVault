package com.example.CloudVault.service;

import com.example.CloudVault.model.FileMetadata;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseFileService {
    private final RealtimeDatabaseService databaseService;
    private final Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Autowired
    public FirebaseFileService(RealtimeDatabaseService databaseService, Storage storage) {
        this.databaseService = databaseService;
        this.storage = storage;
    }
    public FileMetadata uploadFile(MultipartFile file, String userId) throws IOException {
        String fileName = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();

        // Upload to Cloud Storage
        BlobId blobId = BlobId.of(userId, fileId + "_" + fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        // Create metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setId(fileId);
        metadata.setName(fileName);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setUserId(userId);
        metadata.setUploadedAt(String.valueOf(LocalDateTime.now()));
        metadata.setStarred(false);

        // Save metadata to Realtime Database
        databaseService.saveFileMetadata(userId, metadata).join();

        return metadata;
    }

    public List<FileMetadata> getUserFiles(String userId) throws ExecutionException, InterruptedException {
        return databaseService.getUserFiles(userId).get();
    }

    public List<FileMetadata> getStarredFiles(String userId) throws ExecutionException, InterruptedException {
        return databaseService.getStarredFiles(userId).get();
    }

    public void toggleStar(String userId, String fileId) throws ExecutionException, InterruptedException {
        databaseService.toggleStar(userId, fileId).get();
    }

    public byte[] downloadFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        Blob blob = storage.get(BlobId.of(bucketName, file.getGcpPath()));
        return blob.getContent();
    }

    public void deleteFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        // Delete from GCP Storage
        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        storage.delete(blobId);

        // Delete from Realtime Database
        databaseService.deleteFileMetadata(userId, fileId).get();
    }

    public String getFilePreviewUrl(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        Blob blob = storage.get(blobId);

        if (blob == null) {
            throw new RuntimeException("File not found in storage");
        }

        String contentType = file.getContentType();
        if (contentType.startsWith("image/") || 
            contentType.startsWith("video/") || 
            contentType.startsWith("audio/") || 
            contentType.equals("application/pdf")) {
            return blob.signUrl(1, java.util.concurrent.TimeUnit.HOURS).toString();
        }

        throw new RuntimeException("File type not supported for preview");
    }
} 