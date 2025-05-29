"use client";

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { auth } from "../../config/firebase";
import "./Dashboard.css";

import { getAuth } from "firebase/auth";

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [files, setFiles] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const fileInputRef = React.useRef(null);
  const navigate = useNavigate();
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const [previewUrl, setPreviewUrl] = useState("");
  const [previewFile, setPreviewFile] = useState(null);

  // Get current user from Firebase Auth
  const user = auth.currentUser;

  // Protect the dashboard route
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (!user) {
        navigate("/");
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Create user data object from authenticated user
  const userData = {
    id: user?.uid || "",
    name: user?.displayName || "User",
    email: user?.email || "",
    storageUsed: 0,
    storageLimit: 15,
    avatar: user?.photoURL || "/placeholder.svg?height=40&width=40",
    gcpBucket: `cloud-vault-${user?.uid}`,
  };

  useEffect(() => {
    if (user) {
      fetchFilesFromBackend(user.uid);
    }
  }, [user, activeTab]);

  const fetchFilesFromBackend = async (userId) => {
    try {
      // Get fresh token
      const token = await auth.currentUser?.getIdToken(true);
      if (!token) {
        console.error("No token available - user may not be authenticated");
        navigate("/auth");
        return;
      }

      let endpoint = "http://localhost:8080/api/files";
      if (activeTab === "starred") {
        endpoint += "/starred";
      } else if (activeTab === "recent") {
        endpoint += "/recent";
      }

      const headers = {
        Authorization: `Bearer ${token}`,
        // "Content-Type": "application/json",
        "X-User-Id": userId,
      };

      const response = await fetch(endpoint, {
        headers,
        credentials: "include",
      });

      if (!response.ok) {
        if (response.status === 401) {
          console.error("Authentication failed - redirecting to login");
          navigate("/auth");
          return;
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      console.log("Files fetched successfully:", data);
      setFiles(data);
    } catch (error) {
      console.error("Error fetching files:", {
        message: error.message,
        stack: error.stack,
      });
      setFiles([]);
    }
  };

  // Filter files based on active tab and search query
  const filteredFiles = files.filter((file) => {
    const matchesSearch = file.name
      .toLowerCase()
      .includes(searchQuery.toLowerCase());
    if (activeTab === "all") return matchesSearch;
    if (activeTab === "starred") return file.starred && matchesSearch;
    if (activeTab === "recent") {
      // Consider files from the last 7 days as recent
      const sevenDaysAgo = new Date();
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      return new Date(file.lastModified) >= sevenDaysAgo && matchesSearch;
    }
    return matchesSearch;
  });

  // Get file icon based on file type
  const getFileIcon = (type) => {
    switch (type) {
      case "pdf":
        return "fa-solid fa-file-pdf";
      case "excel":
        return "fa-solid fa-file-excel";
      case "image":
        return "fa-solid fa-file-image";
      case "powerpoint":
        return "fa-solid fa-file-powerpoint";
      case "video":
        return "fa-solid fa-file-video";
      case "archive":
        return "fa-solid fa-file-zipper";
      case "word":
        return "fa-solid fa-file-word";
      case "text":
        return "fa-solid fa-file-lines";
      default:
        return "fa-solid fa-file";
    }
  };

  // Calculate storage percentage
  const storagePercentage =
    (userData.storageUsed / userData.storageLimit) * 100;

  // Handle file upload
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setIsUploading(true);
    setUploadProgress(0);

    try {
      const token = await auth.currentUser?.getIdToken(true);
      if (!token) {
        throw new Error("No authentication token available");
      }

      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch("http://localhost:8080/api/files/upload", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-User-Id": user.uid,
        },
        body: formData,
        credentials: "include",
      });

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth");
          return;
        }
        throw new Error(`Upload failed: ${response.status}`);
      }

      const uploadedFile = await response.json();

      await fetchFilesFromBackend(user.uid);

      setFiles((prevFiles) => [uploadedFile, ...prevFiles]);
      setShowUploadModal(false);
    } catch (error) {
      console.error("Error uploading file:", error);
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  // Handle file download
  const handleFileDownload = async (file) => {
    try {
      const token = await auth.currentUser?.getIdToken();
      if (!token) {
        throw new Error("No authentication token available");
      }

      const response = await fetch(
        `http://localhost:8080/api/files/${file.id}/download`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "X-User-Id": user.uid,
          },
          credentials: "include",
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth");
          return;
        }
        throw new Error(`Download failed: ${response.status}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = file.originalName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error("Error downloading file:", error);
    }
  };

  // Toggle star status
  const toggleStar = async (fileId) => {
    try {
      const token = await auth.currentUser?.getIdToken();
      if (!token) {
        throw new Error("No authentication token available");
      }

      const response = await fetch(
        `http://localhost:8080/api/files/${fileId}/star`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
            "X-User-Id": user.uid,
          },
          credentials: "include",
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth");
          return;
        }
        throw new Error(`Failed to toggle star: ${response.status}`);
      }

      // Update the file's starred status in the local state
      setFiles((prevFiles) =>
        prevFiles.map((file) =>
          file.id === fileId ? { ...file, starred: !file.starred } : file
        )
      );
    } catch (error) {
      console.error("Error toggling star:", error);
    }
  };

  // Handle file preview
  const handleFilePreview = async (file) => {
    try {
      const token = await auth.currentUser?.getIdToken();
      if (!token) {
        throw new Error("No authentication token available");
      }

      const response = await fetch(
        `http://localhost:8080/api/files/${file.id}/preview`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "X-User-Id": user.uid,
          },
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("Preview not available");
      }

      const previewUrl = await response.text();
      setPreviewFile(file);
      setPreviewUrl(previewUrl);
      setShowPreviewModal(true);
    } catch (error) {
      console.error("Error getting preview:", error);
    }
  };

  // Handle file deletion
  const handleFileDelete = async (fileId) => {
    if (!window.confirm("Are you sure you want to delete this file?")) {
      return;
    }

    try {
      const token = await auth.currentUser?.getIdToken();
      if (!token) {
        throw new Error("No authentication token available");
      }

      const response = await fetch(
        `http://localhost:8080/api/files/${fileId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
            "X-User-Id": user.uid,
          },
          credentials: "include",
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth");
          return;
        }
        throw new Error("Failed to delete file");
      }

      setFiles((prevFiles) => prevFiles.filter((file) => file.id !== fileId));
    } catch (error) {
      console.error("Error deleting file:", error);
    }
  };

  return (
    <div className="dashboard-container">
      {/* Sidebar */}
      <div className="sidebar">
        <div className="logo">
          <i className="fa-solid fa-cloud"></i>
          <h2>CloudVault</h2>
        </div>

        <div className="sidebar-menu">
          <button
            className={`sidebar-item ${activeTab === "all" ? "active" : ""}`}
            onClick={() => setActiveTab("all")}
          >
            <i className="fa-solid fa-folder"></i>
            <span>All Files</span>
          </button>

          <button
            className={`sidebar-item ${activeTab === "recent" ? "active" : ""}`}
            onClick={() => setActiveTab("recent")}
          >
            <i className="fa-solid fa-clock-rotate-left"></i>
            <span>Recent</span>
          </button>

          <button
            className={`sidebar-item ${
              activeTab === "starred" ? "active" : ""
            }`}
            onClick={() => setActiveTab("starred")}
          >
            <i className="fa-solid fa-star"></i>
            <span>Starred</span>
          </button>

          <button className="sidebar-item">
            <i className="fa-solid fa-trash"></i>
            <span>Trash</span>
          </button>
        </div>

        <div className="storage-info">
          <div className="storage-text">
            <span>Storage</span>
            <span>
              {userData.storageUsed} GB of {userData.storageLimit} GB used
            </span>
          </div>
          <div className="storage-bar">
            <div
              className="storage-fill"
              style={{ width: `${storagePercentage}%` }}
            ></div>
          </div>
          <div className="storage-details">
            <div className="storage-type">
              <span className="storage-color documents"></span>
              <span>Documents</span>
            </div>
            <div className="storage-type">
              <span className="storage-color media"></span>
              <span>Media</span>
            </div>
            <div className="storage-type">
              <span className="storage-color other"></span>
              <span>Other</span>
            </div>
          </div>
        </div>

        <div className="gcp-info">
          <div className="gcp-badge">
            <i className="fa-solid fa-cloud"></i>
            <span>GCP Cloud Storage</span>
          </div>
          <div className="bucket-info">
            <span>Bucket: {userData.gcpBucket}</span>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="main-content">
        {/* Header */}
        <div className="dashboard-header">
          <div className="search-bar">
            <i className="fa-solid fa-search"></i>
            <input
              type="text"
              placeholder="Search files..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="header-actions">
            <button
              className="upload-btn"
              onClick={() => setShowUploadModal(true)}
            >
              <i className="fa-solid fa-cloud-arrow-up"></i>
              <span>Upload</span>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="dashboard-content">
          <h1>Your Files</h1>

          {filteredFiles.length > 0 ? (
            <div className="files-grid">
              {filteredFiles.map((file) => (
                <div className="file-card" key={file.id}>
                  <div className="file-icon">
                    <i className={getFileIcon(file.type)}></i>
                  </div>
                  <div className="file-info">
                    <h3 title={file.name}>{file.name}</h3>
                    <p>
                      <span>{file.size}</span>
                      <span>•</span>
                      <span>{file.lastModified}</span>
                    </p>
                  </div>
                  <div className="file-actions">
                    <button
                      className="action-btn"
                      onClick={() => toggleStar(file.id)}
                      title={
                        file.starred ? "Remove from starred" : "Add to starred"
                      }
                    >
                      <i
                        className={`fa-${
                          file.starred ? "solid" : "regular"
                        } fa-star`}
                      ></i>
                    </button>
                    <button
                      className="action-btn"
                      onClick={() => handleFilePreview(file)}
                      title="Preview file"
                    >
                      <i className="fa-solid fa-eye"></i>
                    </button>
                    <button
                      className="action-btn"
                      onClick={() => handleFileDownload(file)}
                      title="Download file"
                    >
                      <i className="fa-solid fa-download"></i>
                    </button>
                    <button
                      className="action-btn"
                      onClick={() => handleFileDelete(file.id)}
                      title="Delete file"
                    >
                      <i className="fa-solid fa-trash"></i>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <i className="fa-solid fa-folder-open"></i>
              <h2>No files found</h2>
              <p>Upload files or try a different search</p>
              <button
                className="upload-btn-large"
                onClick={() => setShowUploadModal(true)}
              >
                <i className="fa-solid fa-cloud-arrow-up"></i>
                <span>Upload Files</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="upload-modal-overlay">
          <div className="upload-modal">
            <div className="upload-modal-header">
              <h2>Upload to GCP Cloud Storage</h2>
              <button
                className="close-btn"
                onClick={() => {
                  if (!isUploading) {
                    setShowUploadModal(false);
                    setUploadProgress(0);
                  }
                }}
              >
                <i className="fa-solid fa-times"></i>
              </button>
            </div>

            <div className="upload-modal-content">
              <div className="upload-info">
                <i className="fa-solid fa-cloud-arrow-up"></i>
                <p>Files will be uploaded to your GCP Cloud Storage bucket:</p>
                <div className="bucket-name">{userData.gcpBucket}</div>
              </div>

              {isUploading ? (
                <div className="upload-progress">
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{ width: `${uploadProgress}%` }}
                    ></div>
                  </div>
                  <span>{uploadProgress}%</span>
                </div>
              ) : (
                <div className="upload-dropzone">
                  <input
                    type="file"
                    id="file-upload"
                    onChange={handleFileUpload}
                    ref={fileInputRef}
                  />
                  <label htmlFor="file-upload">
                    <i className="fa-solid fa-file-arrow-up"></i>
                    <span>Choose a file or drag it here</span>
                  </label>
                </div>
              )}
            </div>

            <div className="upload-modal-footer">
              <button
                className="cancel-btn"
                onClick={() => {
                  if (!isUploading) {
                    setShowUploadModal(false);
                    setUploadProgress(0);
                  }
                }}
                disabled={isUploading}
              >
                Cancel
              </button>
              <button
                className="upload-btn"
                onClick={() => document.getElementById("file-upload").click()}
                disabled={isUploading}
              >
                {isUploading ? "Uploading..." : "Select File"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* File Preview Modal */}
      {showPreviewModal && previewFile && (
        <div className="preview-modal-overlay">
          <div className="preview-modal">
            <div className="preview-modal-header">
              <h2>{previewFile.originalName}</h2>
              <button
                className="close-btn"
                onClick={() => setShowPreviewModal(false)}
              >
                <i className="fa-solid fa-times"></i>
              </button>
            </div>
            <div className="preview-modal-content">
              {previewFile.contentType.startsWith("image/") && (
                <img src={previewUrl} alt={previewFile.originalName} />
              )}
              {previewFile.contentType.startsWith("video/") && (
                <video controls src={previewUrl} />
              )}
              {previewFile.contentType.startsWith("audio/") && (
                <audio controls src={previewUrl} />
              )}
              {previewFile.contentType === "application/pdf" && (
                <iframe src={previewUrl} title={previewFile.originalName} />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
