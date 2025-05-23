package com.example.CloudVault.controller;

import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.CloudVault.model.FileMetadata;
import com.example.CloudVault.service.FirebaseFileService;
import com.google.cloud.storage.Storage;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class FileController {

    private final FirebaseFileService fileService;
    private final Storage storage;
    
    @Value("${gcp.bucket-name}")
    private String bucketName;
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @GetMapping
    public ResponseEntity<List<FileMetadata>> getAllFiles(
            @RequestHeader("X-User-Id") String userId) {
        try {
            logger.debug("Fetching files for user: {}", userId);
            List<FileMetadata> files = fileService.getUserFiles(userId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error fetching files for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/starred")
    public ResponseEntity<List<FileMetadata>> getStarredFiles(
            @RequestHeader("X-User-Id") String userId) {
        try {
            return ResponseEntity.ok(fileService.getStarredFiles(userId));
        } catch (Exception e) {
            logger.error("Error fetching starred files for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<FileMetadata>> getRecentFiles(
            @RequestHeader("X-User-Id") String userId) {
        try {
            List<FileMetadata> files = fileService.getUserFiles(userId);
            // Filter for recent files (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<FileMetadata> recentFiles = files.stream()
                    .filter(file -> LocalDateTime.parse(file.getUploadedAt()).isAfter(sevenDaysAgo))
                    .toList();
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            logger.error("Error fetching recent files for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from Firebase token
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String userId = decodedToken.getUid();

            // Upload file and get metadata
            FileMetadata metadata = fileService.uploadFile(file, userId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            fileService.deleteFile(userId, fileId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting file: {}, for user: {}", fileId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{fileId}/star")
    public ResponseEntity<Void> toggleStar(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            fileService.toggleStar(userId, fileId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error toggling star for file: {}, user: {}", fileId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            byte[] fileContent = fileService.downloadFile(userId, fileId);
            List<FileMetadata> files = fileService.getUserFiles(userId);
            FileMetadata file = files.stream()
                    .filter(f -> f.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(fileContent);
        } catch (Exception e) {
            logger.error("Error downloading file: {}, for user: {}", fileId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<String> getFilePreviewUrl(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            String previewUrl = fileService.getFilePreviewUrl(userId, fileId);
            return ResponseEntity.ok(previewUrl);
        } catch (Exception e) {
            logger.error("Error getting preview URL for file: {}, user: {}", fileId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test-bucket")
    public ResponseEntity<String> testBucketConnection() {
        try {
            if (storage.get(bucketName) != null) {
                return ResponseEntity.ok("Successfully connected to bucket: " + bucketName);
            } else {
                return ResponseEntity.status(500)
                    .body("Could not find bucket: " + bucketName);
            }
        } catch (Exception e) {
            logger.error("Error testing bucket connection", e);
            return ResponseEntity.status(500)
                .body("Error: " + e.getMessage());
        }
    }
}