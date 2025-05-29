package com.example.CloudVault.service;

import com.example.CloudVault.model.FileMetadata;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
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
        String gcpPath = fileId + "_" + fileName;

        try {
            // Upload to Cloud Storage with proper bucket structure
            BlobId blobId = BlobId.of(bucketName, userId + "/" + gcpPath);
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
            metadata.setUploadedAt(LocalDateTime.now().toString());
            metadata.setStarred(false);
            metadata.setGcpPath(userId + "/" + gcpPath); // Set the GCP path

            // Save metadata to Realtime Database
            databaseService.saveFileMetadata(userId, metadata).join();

            log.info("File uploaded successfully: {} for user: {}", fileName, userId);
            return metadata;
        } catch (Exception e) {
            log.error("Error uploading file: {} for user: {}", fileName, userId, e);
            throw new IOException("Failed to upload file", e);
        }
    }


    public List<FileMetadata> getUserFiles(String userId) throws ExecutionException, InterruptedException {
        try {
            return databaseService.getUserFiles(userId).get();
        } catch (Exception e) {
            log.error("Error retrieving files for user: {}", userId, e);
            throw e;
        }
    }

    public List<FileMetadata> getStarredFiles(String userId) throws ExecutionException, InterruptedException {
        try {
            return databaseService.getStarredFiles(userId).get();
        } catch (Exception e) {
            log.error("Error retrieving starred files for user: {}", userId, e);
            throw e;
        }
    }

    public void toggleStar(String userId, String fileId) throws ExecutionException, InterruptedException {
        try {
            databaseService.toggleStar(userId, fileId).get();
            log.info("Toggled star status for file: {} for user: {}", fileId, userId);
        } catch (Exception e) {
            log.error("Error toggling star for file: {} for user: {}", fileId, userId, e);
            throw e;
        }
    }

    public byte[] downloadFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        try {
            Blob blob = storage.get(BlobId.of(bucketName, file.getGcpPath()));
            if (blob == null) {
                throw new RuntimeException("File not found in storage");
            }
            log.info("File downloaded successfully: {} for user: {}", fileId, userId);
            return blob.getContent();
        } catch (Exception e) {
            log.error("Error downloading file: {} for user: {}", fileId, userId, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    public void deleteFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        try {
            // Delete from GCP Storage
            BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
            boolean deleted = storage.delete(blobId);

            if (!deleted) {
                log.warn("File not found in storage during deletion: {} for user: {}", fileId, userId);
            }

            // Delete from Realtime Database
            databaseService.deleteFileMetadata(userId, fileId).get();

            log.info("File deleted successfully: {} for user: {}", fileId, userId);
        } catch (Exception e) {
            log.error("Error deleting file: {} for user: {}", fileId, userId, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }


    public String getFilePreviewUrl(String userId, String fileId) throws ExecutionException, InterruptedException {
        List<FileMetadata> files = getUserFiles(userId);
        FileMetadata file = files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found or unauthorized access"));

        try {
            BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
            Blob blob = storage.get(blobId);

            if (blob == null) {
                throw new RuntimeException("File not found in storage");
            }

            String contentType = file.getContentType();
            if (contentType != null && (contentType.startsWith("image/") ||
                    contentType.startsWith("video/") ||
                    contentType.startsWith("audio/") ||
                    contentType.equals("application/pdf"))) {

                String url = blob.signUrl(1, java.util.concurrent.TimeUnit.HOURS).toString();
                log.info("Preview URL generated for file: {} for user: {}", fileId, userId);
                return url;
            }

            throw new RuntimeException("File type not supported for preview: " + contentType);
        } catch (Exception e) {
            log.error("Error generating preview URL for file: {} for user: {}", fileId, userId, e);
            throw new RuntimeException("Failed to generate preview URL", e);
        }
    }
}