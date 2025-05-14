package com.example.CloudVault.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.CloudVault.entity.File;
import com.example.CloudVault.repository.FileRepository;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

@Service
public class FileService {

    @Autowired
    private Storage storage;

    @Autowired
    private FileRepository fileRepository;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    public File uploadFile(MultipartFile file, String userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String gcpPath = userId + "/" + uniqueFilename;

        // Upload to GCP
        BlobId blobId = BlobId.of(bucketName, gcpPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();
        storage.create(blobInfo, file.getBytes());

        // Save metadata to database
        File fileEntity = new File();
        fileEntity.setName(uniqueFilename);
        fileEntity.setOriginalName(originalFilename);
        fileEntity.setContentType(file.getContentType());
        fileEntity.setSize(file.getSize());
        fileEntity.setGcpPath(gcpPath);
        fileEntity.setBucketName(bucketName);
        fileEntity.setUserId(userId);
        fileEntity.setStarred(false);

        return fileRepository.save(fileEntity);
    }

    public List<File> getUserFiles(String userId) {
        return fileRepository.findByUserId(userId);
    }

    public List<File> getStarredFiles(String userId) {
        return fileRepository.findByUserIdAndStarredTrue(userId);
    }

    public List<File> getRecentFiles(String userId) {
        return fileRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public void toggleStar(Long fileId) {
        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
        file.setStarred(!file.isStarred());
        fileRepository.save(file);
    }

    public byte[] downloadFile(String userId, Long fileId) {
        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        Blob blob = storage.get(BlobId.of(bucketName, file.getGcpPath()));
        return blob.getContent();
    }

    public void deleteFile(String userId, Long fileId) {
        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Delete from GCP
        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        storage.delete(blobId);

        // Delete from database
        fileRepository.delete(file);
    }

    public String getFilePreviewUrl(String userId, Long fileId) {
        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Generate a signed URL that expires in 1 hour
        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        Blob blob = storage.get(blobId);
        
        if (blob == null) {
            throw new RuntimeException("File not found in storage");
        }

        // Check if file type is previewable
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