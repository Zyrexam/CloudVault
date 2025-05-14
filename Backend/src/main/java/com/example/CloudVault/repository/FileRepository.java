package com.example.CloudVault.repository;

import com.example.CloudVault.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserId(String userId);
    List<File> findByUserIdAndStarredTrue(String userId);
    List<File> findByUserIdOrderByUploadedAtDesc(String userId);
} 