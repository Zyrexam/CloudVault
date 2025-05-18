package com.example.CloudVault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String id;
    private String name;
    private String originalName;
    private String contentType;
    private Long size;
    private String gcpPath;
    private String bucketName;
    private String userId;
    private String uploadedAt; // Use ISO string for compatibility
    private boolean starred;
}

