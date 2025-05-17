package com.example.CloudVault.service;

import com.example.CloudVault.model.FileModel;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.storage.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseFileService {

    private final Storage storage;
    
    @Value("${gcp.bucket-name}")
    private String bucketName;

    public FirebaseFileService(Storage storage) {
        this.storage = storage;
    }

    public FileModel uploadFile(MultipartFile file, String userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String gcpPath = userId + "/" + uniqueFilename;

        // Upload to GCP Storage
        BlobId blobId = BlobId.of(bucketName, gcpPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();
        storage.create(blobInfo, file.getBytes());

        // Create file metadata
        FileModel fileModel = FileModel.builder()
            .id(UUID.randomUUID().toString())
            .name(uniqueFilename)
            .originalName(originalFilename)
            .contentType(file.getContentType())
            .size(file.getSize())
            .gcpPath(gcpPath)
            .bucketName(bucketName)
            .userId(userId)
            .uploadedAt(LocalDateTime.now())
            .starred(false)
            .build();

        // Save to Firestore
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection("files").document(fileModel.getId()).set(fileModel);

        return fileModel;
    }

    public List<FileModel> getUserFiles(String userId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = firestore.collection("files")
            .whereEqualTo("userId", userId)
            .get();

        List<FileModel> files = new ArrayList<>();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            files.add(document.toObject(FileModel.class));
        }
        return files;
    }

    public List<FileModel> getStarredFiles(String userId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = firestore.collection("files")
            .whereEqualTo("userId", userId)
            .whereEqualTo("starred", true)
            .get();

        List<FileModel> files = new ArrayList<>();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            files.add(document.toObject(FileModel.class));
        }
        return files;
    }

    public void toggleStar(String fileId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        FileModel file = firestore.collection("files").document(fileId).get().get().toObject(FileModel.class);
        if (file != null) {
            file.setStarred(!file.isStarred());
            firestore.collection("files").document(fileId).set(file);
        }
    }

    public byte[] downloadFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        FileModel file = firestore.collection("files").document(fileId).get().get().toObject(FileModel.class);
        
        if (file == null || !file.getUserId().equals(userId)) {
            throw new RuntimeException("File not found or unauthorized access");
        }

        Blob blob = storage.get(BlobId.of(bucketName, file.getGcpPath()));
        return blob.getContent();
    }

    public void deleteFile(String userId, String fileId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        FileModel file = firestore.collection("files").document(fileId).get().get().toObject(FileModel.class);
        
        if (file == null || !file.getUserId().equals(userId)) {
            throw new RuntimeException("File not found or unauthorized access");
        }

        // Delete from GCP Storage
        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        storage.delete(blobId);

        // Delete from Firestore
        firestore.collection("files").document(fileId).delete();
    }

    public String getFilePreviewUrl(String userId, String fileId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        FileModel file = firestore.collection("files").document(fileId).get().get().toObject(FileModel.class);
        
        if (file == null || !file.getUserId().equals(userId)) {
            throw new RuntimeException("File not found or unauthorized access");
        }

        BlobId blobId = BlobId.of(bucketName, file.getGcpPath());
        Blob blob = storage.get(blobId);
        
        if (blob == null) {
            throw new RuntimeException("File not found in storage");
        }

        String contentType = file.getContentType();
        if (contentType.startsWith("image/") || 
            contentType.startsWith("video/") || 
            contentType.startsWith("audio/") || 
            contentType.equals("application/pdf")) {
            
            return blob.signUrl(1, java.util.concurrent.TimeUnit.HOURS).toString();
        }

        throw new RuntimeException("File type not supported for preview");
    }
} 