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
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("=== FILE UPLOAD REQUEST ===");
        logger.info("User ID: {}", userId);
        logger.info("Auth header present: {}", authHeader != null);
        logger.info("File name: {}", file != null ? file.getOriginalFilename() : "null");
        logger.info("File size: {}", file != null ? file.getSize() : "null");
        logger.info("Content type: {}", file != null ? file.getContentType() : "null");

        try {
            // Validate inputs
            if (file == null || file.isEmpty()) {
                logger.error("File is null or empty");
                return ResponseEntity.badRequest().build();
            }

            if (userId == null || userId.trim().isEmpty()) {
                logger.error("User ID is null or empty");
                return ResponseEntity.badRequest().build();
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.error("Invalid authorization header");
                return ResponseEntity.badRequest().build();
            }

            // Optional: Verify the token matches the user ID for extra security
            logger.info("Attempting to verify Firebase token...");
            String token = authHeader.replace("Bearer ", "");

            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                logger.info("Token verified successfully. UID: {}", decodedToken.getUid());

                // Verify that the token's UID matches the provided user ID
                if (!decodedToken.getUid().equals(userId)) {
                    logger.error("Token UID ({}) does not match provided user ID ({})",
                            decodedToken.getUid(), userId);
                    return ResponseEntity.status(403).build();
                }
            } catch (Exception tokenException) {
                logger.error("Firebase token verification failed", tokenException);
                return ResponseEntity.status(401).build();
            }

            logger.info("Attempting to upload file via FirebaseFileService...");
            // Upload file and get metadata
            FileMetadata metadata = fileService.uploadFile(file, userId);
            logger.info("File uploaded successfully. Metadata: {}", metadata);

            return ResponseEntity.ok(metadata);

        } catch (Exception e) {
            logger.error("Error uploading file for user: {}", userId, e);
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
//    @PostMapping("/upload")
//    public ResponseEntity<FileMetadata> uploadFile(
//            @RequestParam("file") MultipartFile file,
//            @RequestHeader("X-User-Id") String userId,
//            @RequestHeader("Authorization") String authHeader) {
//        try {
//            // Optional: Verify the token matches the user ID for extra security
//            String token = authHeader.replace("Bearer ", "");
//            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
//
//            // Verify that the token's UID matches the provided user ID
//            if (!decodedToken.getUid().equals(userId)) {
//                logger.error("Token UID does not match provided user ID");
//                return ResponseEntity.status(403).build();
//            }
//
//            // Upload file and get metadata
//            FileMetadata metadata = fileService.uploadFile(file, userId);
//            return ResponseEntity.ok(metadata);
//        } catch (Exception e) {
//            logger.error("Error uploading file for user: {}", userId, e);
//            return ResponseEntity.badRequest().build();
//        }
//    }
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