package com.example.CloudVault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileModel {
    private String id;
    private String name;
    private String originalName;
    private String contentType;
    private Long size;
    private String gcpPath;
    private String bucketName;
    private String userId;
    private LocalDateTime uploadedAt;
    private boolean starred;
} 