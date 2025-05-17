package com.example.CloudVault.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.CloudVault.model.FileModel;
import com.example.CloudVault.service.FirebaseFileService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class FileController {

    private final FirebaseFileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

//    public ResponseEntity<List<FileModel>> getAllFiles(
//            @RequestHeader("X-User-Id") String userId,
//            @RequestHeader("Authorization") String token) {
//        try {
//            logger.debug("Fetching files for user: {}", userId);
//            List<FileModel> files = fileService.getUserFiles(userId);
//            return ResponseEntity.ok(files);
//        } catch (Exception e) {
//            logger.error("Error fetching files for user: {}", userId, e);
//            return ResponseEntity.badRequest().build();
//        }
//    }

    @GetMapping("/starred")
    public ResponseEntity<List<FileModel>> getStarredFiles(
            @RequestHeader("X-User-Id") String userId) {
        try {
            return ResponseEntity.ok(fileService.getStarredFiles(userId));
        } catch (Exception e) {
            logger.error("Error fetching starred files for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<FileModel>> getRecentFiles(
            @RequestHeader("X-User-Id") String userId) {
        try {
            List<FileModel> files = fileService.getUserFiles(userId);
            // Filter for recent files (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<FileModel> recentFiles = files.stream()
                    .filter(file -> file.getUploadedAt().isAfter(sevenDaysAgo))
                    .toList();
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            logger.error("Error fetching recent files for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<FileModel> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {
        try {
            FileModel uploadedFile = fileService.uploadFile(file, userId);
            return ResponseEntity.ok(uploadedFile);
        } catch (Exception e) {
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
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FileModel>> getUserFiles(@RequestHeader("X-User-Id") String userId) {
        try {
            return ResponseEntity.ok(fileService.getUserFiles(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    

    @PostMapping("/{fileId}/star")
    public ResponseEntity<Void> toggleStar(@PathVariable String fileId) {
        try {
            fileService.toggleStar(fileId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            byte[] fileContent = fileService.downloadFile(userId, fileId);
            List<FileModel> files = fileService.getUserFiles(userId);
            FileModel file = files.stream()
                    .filter(f -> f.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(fileContent);
        } catch (Exception e) {
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
            return ResponseEntity.badRequest().build();
        }
    }
    

}