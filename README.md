# 🌩️ CloudVault

A secure cloud storage solution leveraging Spring Boot, React, Firebase, and Google Cloud Platform (GCP) services. Store, manage, and share your files securely in the cloud.

## 🚀 Key Features

- 📁 File Management
  - Upload and download files
  - Star important files
  - View file metadata
  - Real-time storage usage tracking
- 🔐 Secure Authentication
  - Firebase Authentication
  - JWT token validation
  - Role-based access control
- ☁️ Cloud Integration
  - GCP Cloud Storage for files
  - Firebase Realtime Database for metadata
  - Real-time updates and sync

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Java Version**: 17
- **Key Dependencies**:
  ```xml
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>com.google.firebase</groupId>
      <artifactId>firebase-admin</artifactId>
      <version>9.2.0</version>
    </dependency>
    <dependency>
      <groupId>com.google.cloud</groupId>
      <artifactId>google-cloud-storage</artifactId>
    </dependency>
  </dependencies>
  

## 🚀 Getting Started

### Prerequisites
1. Java 17+
2. Node.js 16+
3. Firebase Project
4. GCP Project with Storage enabled

### Local Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/Zyrexam/cloudvault.git
   cd cloudvault
   ```

2. **Backend Configuration**
   ```bash
   cd Backend
   # Copy service account template
   cp src/main/resources/service-account-key.example.json src/main/resources/service-account-key.json
   # Update with your credentials
   ```

3. **Frontend Configuration**
   ```bash
   cd frontend
   cp .env.example .env
   # Update Firebase configuration
   ```

### Running the Application

1. **Start Backend**
   ```bash
   cd Backend
   mvn spring-boot:run
   ```

2. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## 🔧 Configuration Files

### Backend (application.yml)
```yaml

spring:
  application:
    name: Backend
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

server:
  port: 8080
  servlet:
    context-path: /

firebase:
  bucket-name:  
  image-url: https://storage.googleapis.com/${firebase.bucket-name}/%s

cors:
  allowed-origins: http://localhost:5173
  allowed-credentials: true
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  exposed-headers: "*"

gcp:
  project-id: 
  bucket-name: 
  credentials:
    location: src/main/resources/service-account-key.json

logging:
  level:
    com.example.CloudVault: DEBUG
    com.google.cloud: DEBUG


```

### Frontend (.env)
```env
VITE_FIREBASE_API_KEY= 
VITE_FIREBASE_AUTH_DOMAIN=
VITE_FIREBASE_STORAGE_BUCKET=
VITE_FIREBASE_PROJECT_ID=
VITE_FIREBASE_MESSAGING_SENDER_ID=
VITE_FIREBASE_APP_ID=
VITE_FIREBASE_MEASUREMENT_ID=
```

## 🔒 Security Notes

- Service account credentials are never exposed to frontend
- Files are stored in private GCP buckets
- Firebase Authentication handles user management
- CORS is configured for frontend-backend communication

## 📚 API Documentation

### File Operations
- `POST /api/files/upload` - Upload new file
- `GET /api/files` - List all files
- `GET /api/files/{fileId}` - Download file
- `DELETE /api/files/{fileId}` - Delete file
- `PUT /api/files/{fileId}/star` - Toggle star

## 🤝 Contributing
1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add: Amazing Feature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
