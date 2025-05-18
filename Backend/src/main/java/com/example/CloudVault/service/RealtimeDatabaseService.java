package com.example.CloudVault.service;

import com.google.firebase.database.*;
import com.example.CloudVault.model.FileMetadata;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RealtimeDatabaseService {
    private final DatabaseReference database;

    public RealtimeDatabaseService() {
        this.database = FirebaseDatabase.getInstance().getReference();
    }

    public CompletableFuture<Void> saveFileMetadata(String userId, FileMetadata metadata) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.child("files").child(userId).child(metadata.getId()).setValueAsync(metadata)
            .addListener(() -> future.complete(null), Runnable::run);
        return future;
    }

    public CompletableFuture<List<FileMetadata>> getUserFiles(String userId) {
        CompletableFuture<List<FileMetadata>> future = new CompletableFuture<>();
        database.child("files").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<FileMetadata> files = new ArrayList<>();
                    for (DataSnapshot fileSnapshot : snapshot.getChildren()) {
                        FileMetadata file = fileSnapshot.getValue(FileMetadata.class);
                        if (file != null) {
                            files.add(file);
                        }
                    }
                    future.complete(files);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });
        return future;
    }

    public CompletableFuture<List<FileMetadata>> getStarredFiles(String userId) {
        return getUserFiles(userId).thenApply(files -> 
            files.stream()
                .filter(FileMetadata::isStarred)
                .toList()
        );
    }

    
    public CompletableFuture<Void> toggleStar(String userId, String fileId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference fileRef = database.child("files").child(userId).child(fileId);
        
        fileRef.child("starred").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean currentValue = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                fileRef.child("starred").setValueAsync(!currentValue)
                    .addListener(() -> future.complete(null), Runnable::run);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });
        
        return future;
    }
    public CompletableFuture<Void> deleteFileMetadata(String userId, String fileId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.child("files").child(userId).child(fileId).removeValueAsync()
            .addListener(() -> future.complete(null), Runnable::run);
        return future;
    }
} 