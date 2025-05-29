package com.example.CloudVault.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @NotBlank
    private String id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    private String contentType;

    @Positive
    private Long size;

    private String originalName;

    private String gcpPath;
    private String bucketName;
    private String userId;
    private String uploadedAt; // Use ISO string for compatibility
    private boolean starred;
}
