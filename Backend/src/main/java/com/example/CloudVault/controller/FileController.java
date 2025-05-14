package com.example.CloudVault.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.CloudVault.entity.File;
import com.example.CloudVault.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {
        try {
            File uploadedFile = fileService.uploadFile(file, userId);
            return ResponseEntity.ok(uploadedFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            fileService.deleteFile(userId, fileId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<File>> getUserFiles(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(fileService.getUserFiles(userId));
    }

    @GetMapping("/starred")
    public ResponseEntity<List<File>> getStarredFiles(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(fileService.getStarredFiles(userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<File>> getRecentFiles(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(fileService.getRecentFiles(userId));
    }

    @PostMapping("/{fileId}/star")
    public ResponseEntity<Void> toggleStar(@PathVariable Long fileId) {
        fileService.toggleStar(fileId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            byte[] fileContent = fileService.downloadFile(userId, fileId);
            File file = fileService.getUserFiles(userId).stream()
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
            @PathVariable Long fileId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            String previewUrl = fileService.getFilePreviewUrl(userId, fileId);
            return ResponseEntity.ok(previewUrl);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 