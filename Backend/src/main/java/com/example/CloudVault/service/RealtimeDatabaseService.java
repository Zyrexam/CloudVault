package com.example.CloudVault.service;

import com.google.firebase.database.*;
import com.example.CloudVault.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RealtimeDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeDatabaseService.class);
    private final DatabaseReference database;

    public RealtimeDatabaseService() {
        try {
            this.database = FirebaseDatabase.getInstance().getReference();
            logger.info("RealtimeDatabaseService initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize RealtimeDatabaseService", e);
            throw new RuntimeException("Failed to initialize Firebase Realtime Database", e);
        }
    }

    public CompletableFuture<Void> saveFileMetadata(String userId, FileMetadata metadata) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID cannot be null or empty"));
            return future;
        }

        if (metadata == null || metadata.getId() == null) {
            future.completeExceptionally(new IllegalArgumentException("Metadata and file ID cannot be null"));
            return future;
        }

        try {
            database.child("files").child(userId).child(metadata.getId()).setValueAsync(metadata)
                    .addListener(() -> {
                        logger.info("File metadata saved successfully for user: {} file: {}", userId, metadata.getId());
                        future.complete(null);
                    }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error saving file metadata for user: {} file: {}", userId, metadata.getId(), e);
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<List<FileMetadata>> getUserFiles(String userId) {
        CompletableFuture<List<FileMetadata>> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID cannot be null or empty"));
            return future;
        }

        try {
            database.child("files").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            List<FileMetadata> files = new ArrayList<>();
                            try {
                                for (DataSnapshot fileSnapshot : snapshot.getChildren()) {
                                    FileMetadata file = fileSnapshot.getValue(FileMetadata.class);
                                    if (file != null) {
                                        // Ensure all required fields are set
                                        if (file.getId() == null) {
                                            file.setId(fileSnapshot.getKey());
                                        }
                                        files.add(file);
                                    }
                                }
                                logger.info("Retrieved {} files for user: {}", files.size(), userId);
                                future.complete(files);
                            } catch (Exception e) {
                                logger.error("Error processing files data for user: {}", userId, e);
                                future.completeExceptionally(e);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            logger.error("Database query cancelled for user: {} - {}", userId, error.getMessage());
                            future.completeExceptionally(error.toException());
                        }
                    });
        } catch (Exception e) {
            logger.error("Error querying files for user: {}", userId, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<List<FileMetadata>> getStarredFiles(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            CompletableFuture<List<FileMetadata>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("User ID cannot be null or empty"));
            return future;
        }

        return getUserFiles(userId).thenApply(files -> {
            try {
                List<FileMetadata> starredFiles = files.stream()
                        .filter(file -> file != null && file.isStarred())
                        .toList();
                logger.info("Retrieved {} starred files for user: {}", starredFiles.size(), userId);
                return starredFiles;
            } catch (Exception e) {
                logger.error("Error filtering starred files for user: {}", userId, e);
                throw new RuntimeException("Error processing starred files", e);
            }
        });
    }

    public CompletableFuture<Void> toggleStar(String userId, String fileId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty() || fileId == null || fileId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID and File ID cannot be null or empty"));
            return future;
        }

        try {
            DatabaseReference fileRef = database.child("files").child(userId).child(fileId);

            fileRef.child("starred").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        boolean currentValue = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        boolean newValue = !currentValue;

                        fileRef.child("starred").setValueAsync(newValue)
                                .addListener(() -> {
                                    logger.info("Toggled star status to {} for file: {} user: {}", newValue, fileId, userId);
                                    future.complete(null);
                                }, Runnable::run);
                    } catch (Exception e) {
                        logger.error("Error toggling star for file: {} user: {}", fileId, userId, e);
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("Database query cancelled for toggle star file: {} user: {} - {}", fileId, userId, error.getMessage());
                    future.completeExceptionally(error.toException());
                }
            });
        } catch (Exception e) {
            logger.error("Error in toggleStar for file: {} user: {}", fileId, userId, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<Void> deleteFileMetadata(String userId, String fileId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty() || fileId == null || fileId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID and File ID cannot be null or empty"));
            return future;
        }

        try {
            database.child("files").child(userId).child(fileId).removeValueAsync()
                    .addListener(() -> {
                        logger.info("File metadata deleted successfully for user: {} file: {}", userId, fileId);
                        future.complete(null);
                    }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error deleting file metadata for user: {} file: {}", userId, fileId, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Check if a file exists for a user
     */
    public CompletableFuture<Boolean> fileExists(String userId, String fileId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty() || fileId == null || fileId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID and File ID cannot be null or empty"));
            return future;
        }

        try {
            database.child("files").child(userId).child(fileId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            boolean exists = snapshot.exists();
                            logger.debug("File exists check for user: {} file: {} result: {}", userId, fileId, exists);
                            future.complete(exists);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            logger.error("Database query cancelled for file exists check user: {} file: {} - {}",
                                    userId, fileId, error.getMessage());
                            future.completeExceptionally(error.toException());
                        }
                    });
        } catch (Exception e) {
            logger.error("Error checking file existence for user: {} file: {}", userId, fileId, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Get file metadata by ID
     */
    public CompletableFuture<FileMetadata> getFileMetadata(String userId, String fileId) {
        CompletableFuture<FileMetadata> future = new CompletableFuture<>();

        if (userId == null || userId.trim().isEmpty() || fileId == null || fileId.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("User ID and File ID cannot be null or empty"));
            return future;
        }

        try {
            database.child("files").child(userId).child(fileId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            try {
                                FileMetadata metadata = snapshot.getValue(FileMetadata.class);
                                if (metadata != null && metadata.getId() == null) {
                                    metadata.setId(snapshot.getKey());
                                }
                                logger.debug("Retrieved file metadata for user: {} file: {}", userId, fileId);
                                future.complete(metadata);
                            } catch (Exception e) {
                                logger.error("Error processing file metadata for user: {} file: {}", userId, fileId, e);
                                future.completeExceptionally(e);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            logger.error("Database query cancelled for get file metadata user: {} file: {} - {}",
                                    userId, fileId, error.getMessage());
                            future.completeExceptionally(error.toException());
                        }
                    });
        } catch (Exception e) {
            logger.error("Error getting file metadata for user: {} file: {}", userId, fileId, e);
            future.completeExceptionally(e);
        }

        return future;
    }
}