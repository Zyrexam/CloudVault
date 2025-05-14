package com.example.CloudVault.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String gcpPath;

    @Column(nullable = false)
    private String bucketName;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private boolean starred;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
} 