package com.example.CloudVault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileUrl;
    
    @Column(nullable = false)
    private String contentType;
    
    private Long size;
    
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    
    @Column(nullable = false)
    private String uploadedBy;
} 