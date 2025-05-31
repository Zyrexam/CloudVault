"use client"

import React, { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { auth } from "../../config/firebase"
import "./dashboard.css"

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState("all")
  const [searchQuery, setSearchQuery] = useState("")
  const [files, setFiles] = useState([])
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [showUploadModal, setShowUploadModal] = useState(false)
  const [viewMode, setViewMode] = useState("grid")
  const fileInputRef = React.useRef(null)
  const navigate = useNavigate()
  const [showPreviewModal, setShowPreviewModal] = useState(false)
  const [previewUrl, setPreviewUrl] = useState("")
  const [previewFile, setPreviewFile] = useState(null)
  const [isDownloading, setIsDownloading] = useState({})
  const [dragActive, setDragActive] = useState(false)
  
  // Add storage state variables
  const [storageStats, setStorageStats] = useState({
    total: 0,
    documents: 0,
    media: 0,
    other: 0,
    percentage: 0
  })

  // Get current user from Firebase Auth
  const user = auth.currentUser

  // Protect the dashboard route
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (!user) {
        navigate("/")
      }
    })

    return () => unsubscribe()
  }, [navigate])

  // Create user data object from authenticated user
  const userData = {
    id: user?.uid || "",
    name: user?.displayName || "User",
    email: user?.email || "",
    storageLimit: 15,
    avatar: user?.photoURL || "/placeholder.svg?height=40&width=40",
    gcpBucket: `cloud-vault-${user?.uid}`,
  }

  const calculateStorageUsed = (files) => {
    if (!files || files.length === 0) {
      return {
        total: 0,
        documents: 0,
        media: 0,
        other: 0,
        percentage: 0
      };
    }

    let totalStorageBytes = 0;
    let documentStorageBytes = 0;
    let mediaStorageBytes = 0;
    let otherStorageBytes = 0;

    files.forEach(file => {
      // Make sure we're parsing the size correctly
      const fileSize = parseInt(file.size) || 0;
      console.log(`Processing file: ${file.name}, size: ${fileSize} bytes`);
      
      totalStorageBytes += fileSize;

      // Categorize by file type
      if (file.contentType?.startsWith('image/') || 
          file.contentType?.startsWith('video/') || 
          file.contentType?.startsWith('audio/')) {
        mediaStorageBytes += fileSize;
      } else if (file.contentType?.includes('pdf') || 
                 file.contentType?.includes('document') || 
                 file.contentType?.includes('text/')) {
        documentStorageBytes += fileSize;
      } else {
        otherStorageBytes += fileSize;
      }
    });

    // Convert to GB with 2 decimal places
    const bytesToGB = bytes => Math.round((bytes / (1024 * 1024 * 1024)) * 100) / 100;
    const totalStorageLimit = 15; // 15GB limit
    const percentage = (bytesToGB(totalStorageBytes) / totalStorageLimit) * 100;

    const stats = {
      total: bytesToGB(totalStorageBytes),
      documents: bytesToGB(documentStorageBytes),
      media: bytesToGB(mediaStorageBytes),
      other: bytesToGB(otherStorageBytes),
      percentage: Math.round(percentage * 100) / 100
    };

    console.log('Calculated storage stats:', stats);
    return stats;
  };

  useEffect(() => {
    if (user) {
      fetchFilesFromBackend(user.uid)
    }
  }, [user, activeTab])

  const fetchFilesFromBackend = async (userId) => {
    try {
      const token = await auth.currentUser?.getIdToken(true)
      if (!token) {
        console.error("No token available - user may not be authenticated")
        navigate("/auth")
        return
      }

      let endpoint = "http://localhost:8080/api/files"
      if (activeTab === "starred") {
        endpoint += "/starred"
      } else if (activeTab === "recent") {
        endpoint += "/recent"
      }

      const headers = {
        Authorization: `Bearer ${token}`,
        "X-User-Id": userId,
      }

      const response = await fetch(endpoint, {
        headers,
        credentials: "include",
      })

      if (!response.ok) {
        if (response.status === 401) {
          console.error("Authentication failed - redirecting to login")
          navigate("/auth")
          return
        }
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      console.log("Files fetched successfully:", data)
      setFiles(data)
      
      // Calculate and set storage stats
      const calculatedStorageStats = calculateStorageUsed(data)
      setStorageStats(calculatedStorageStats)

    } catch (error) {
      console.error("Error fetching files:", {
        message: error.message,
        stack: error.stack,
      })
      setFiles([])
      // Reset storage stats on error
      setStorageStats({
        total: 0,
        documents: 0,
        media: 0,
        other: 0,
        percentage: 0
      })
    }
  }

  // Filter files based on active tab and search query
  const filteredFiles = files.filter((file) => {
    const matchesSearch = file.name.toLowerCase().includes(searchQuery.toLowerCase())
    if (activeTab === "all") return matchesSearch
    if (activeTab === "starred") return file.starred && matchesSearch
    if (activeTab === "recent") {
      const sevenDaysAgo = new Date()
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
      return new Date(file.lastModified) >= sevenDaysAgo && matchesSearch
    }
    if (activeTab === "trash") return file.deleted && matchesSearch
    return matchesSearch
  })

  // Get file icon based on file type
  const getFileIcon = (type) => {
    switch (type) {
      case "pdf":
        return "fa-solid fa-file-pdf"
      case "excel":
        return "fa-solid fa-file-excel"
      case "image":
        return "fa-solid fa-file-image"
      case "powerpoint":
        return "fa-solid fa-file-powerpoint"
      case "video":
        return "fa-solid fa-file-video"
      case "archive":
        return "fa-solid fa-file-zipper"
      case "word":
        return "fa-solid fa-file-word"
      case "text":
        return "fa-solid fa-file-lines"
      default:
        return "fa-solid fa-file"
    }
  }

  // Handle drag events
  const handleDrag = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true)
    } else if (e.type === "dragleave") {
      setDragActive(false)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload({ target: { files: [e.dataTransfer.files[0]] } })
    }
  }

  // Enhanced file upload with progress tracking
  const handleFileUpload = async (event) => {
    const file = event.target.files ? event.target.files[0] : event
    if (!file) return

    setIsUploading(true)
    setUploadProgress(0)

    try {
      console.log("Starting file upload for:", file.name)

      if (!user?.uid) {
        console.error("No user UID available")
        throw new Error("User not authenticated")
      }

      // Get fresh token for upload
      const token = await auth.currentUser?.getIdToken(true)
      if (!token) {
        console.error("No access token available")
        throw new Error("No authentication token")
      }

      const formData = new FormData()
      formData.append("file", file)

      const headers = {
        "X-User-Id": user.uid,
        Authorization: `Bearer ${token}`,
      }

      // Create XMLHttpRequest for progress tracking
      const xhr = new XMLHttpRequest()

      // Track upload progress
      xhr.upload.addEventListener("progress", (event) => {
        if (event.lengthComputable) {
          const progress = Math.round((event.loaded / event.total) * 100)
          setUploadProgress(progress)
        }
      })

      // Handle upload completion
      xhr.addEventListener("load", async () => {
        if (xhr.status === 200) {
          console.log("Upload successful")
          await fetchFilesFromBackend(user.uid)
          setShowUploadModal(false)
        } else {
          throw new Error(`Upload failed: ${xhr.status}`)
        }
      })

      xhr.addEventListener("error", () => {
        throw new Error("Upload failed")
      })

      // Set headers and send request
      Object.keys(headers).forEach((key) => {
        xhr.setRequestHeader(key, headers[key])
      })

      xhr.open("POST", "http://localhost:8080/api/files/upload")
      xhr.send(formData)
    } catch (error) {
      console.error("Error uploading file:", error)
      alert(`Upload failed: ${error.message}`)
      setShowUploadModal(false)
    } finally {
      setIsUploading(false)
      setUploadProgress(0)
      if (fileInputRef.current) {
        fileInputRef.current.value = ""
      }
    }
  }

  // Enhanced file download with loading state
  const handleFileDownload = async (file) => {
    setIsDownloading((prev) => ({ ...prev, [file.id]: true }))

    try {
      const token = await auth.currentUser?.getIdToken()
      if (!token) {
        throw new Error("No authentication token available")
      }

      const response = await fetch(`http://localhost:8080/api/files/${file.id}/download`, {
        headers: {
          Authorization: `Bearer ${token}`,
          "X-User-Id": user.uid,
        },
        credentials: "include",
      })

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth")
          return
        }
        throw new Error(`Download failed: ${response.status}`)
      }

      // Use streaming for faster downloads
      const reader = response.body.getReader()
      const contentLength = +response.headers.get("Content-Length")

      let receivedLength = 0
      const chunks = []

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          break
        }

        chunks.push(value)
        receivedLength += value.length
      }

      const blob = new Blob(chunks)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = file.originalName || file.name || "file"
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error("Error downloading file:", error)
      alert("Download failed. Please try again.")
    } finally {
      setIsDownloading((prev) => ({ ...prev, [file.id]: false }))
    }
  }

  // Toggle star status
  const toggleStar = async (fileId) => {
    try {
      const token = await auth.currentUser?.getIdToken()
      if (!token) {
        throw new Error("No authentication token available")
      }

      const response = await fetch(`http://localhost:8080/api/files/${fileId}/star`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "X-User-Id": user.uid,
        },
        credentials: "include",
      })

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth")
          return
        }
        throw new Error(`Failed to toggle star: ${response.status}`)
      }

      setFiles((prevFiles) =>
        prevFiles.map((file) => (file.id === fileId ? { ...file, starred: !file.starred } : file)),
      )
    } catch (error) {
      console.error("Error toggling star:", error)
    }
  }

  // Handle file preview
  const handleFilePreview = async (file) => {
    try {
      const token = await auth.currentUser?.getIdToken()
      if (!token) {
        throw new Error("No authentication token available")
      }

      const response = await fetch(`http://localhost:8080/api/files/${file.id}/preview`, {
        headers: {
          Authorization: `Bearer ${token}`,
          "X-User-Id": user.uid,
        },
        credentials: "include",
      })

      if (!response.ok) {
        throw new Error("Preview not available")
      }

      const previewUrl = await response.text()
      setPreviewFile(file)
      setPreviewUrl(previewUrl)
      setShowPreviewModal(true)
    } catch (error) {
      console.error("Error getting preview:", error)
      alert("Preview not available for this file type")
    }
  }

  // Handle file deletion
  const handleFileDelete = async (fileId) => {
    if (!window.confirm("Are you sure you want to delete this file?")) {
      return
    }

    try {
      const token = await auth.currentUser?.getIdToken()
      if (!token) {
        throw new Error("No authentication token available")
      }

      const response = await fetch(`http://localhost:8080/api/files/${fileId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-User-Id": user.uid,
        },
        credentials: "include",
      })

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/auth")
          return
        }
        throw new Error("Failed to delete file")
      }

      setFiles((prevFiles) => prevFiles.filter((file) => file.id !== fileId))
    } catch (error) {
      console.error("Error deleting file:", error)
    }
  }

  // Get empty state content based on active tab
  const getEmptyStateContent = () => {
    switch (activeTab) {
      case "trash":
        return {
          icon: "fa-solid fa-trash-can",
          title: "Trash is empty",
          description: "Deleted files will appear here",
          showUploadButton: false,
        }
      case "starred":
        return {
          icon: "fa-solid fa-star",
          title: "No starred files",
          description: "Star files to find them easily later",
          showUploadButton: true,
        }
      case "recent":
        return {
          icon: "fa-solid fa-clock-rotate-left",
          title: "No recent files",
          description: "Files modified in the last 7 days will appear here",
          showUploadButton: true,
        }
      default:
        return {
          icon: "fa-solid fa-folder-open",
          title: "No files found",
          description: searchQuery ? "Try a different search term" : "Upload files to get started",
          showUploadButton: !searchQuery,
        }
    }
  }

  const emptyState = getEmptyStateContent()

  return (
    <div
      className="dashboard-container"
      onDragEnter={handleDrag}
      onDragLeave={handleDrag}
      onDragOver={handleDrag}
      onDrop={handleDrop}
    >
      {/* Sidebar */}
      <div className="sidebar">
        <div className="logo">
          <i className="fa-solid fa-cloud"></i>
          <h2>CloudVault</h2>
        </div>

        <div className="sidebar-menu">
          <button className={`sidebar-item ${activeTab === "all" ? "active" : ""}`} onClick={() => setActiveTab("all")}>
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
            className={`sidebar-item ${activeTab === "starred" ? "active" : ""}`}
            onClick={() => setActiveTab("starred")}
          >
            <i className="fa-solid fa-star"></i>
            <span>Starred</span>
          </button>

          <button
            className={`sidebar-item ${activeTab === "trash" ? "active" : ""}`}
            onClick={() => setActiveTab("trash")}
          >
            <i className="fa-solid fa-trash"></i>
            <span>Trash</span>
          </button>
        </div>

        <div className="storage-info" data-percentage={`${Math.round(storageStats.percentage)}%`}>
          <div className="storage-text">
            <span>Storage</span>
            <span>
              {storageStats.total} GB of {userData.storageLimit} GB used
            </span>
          </div>
          <div className="storage-bar">
            <div className="storage-fill" style={{ width: `${storageStats.percentage}%` }}></div>
          </div>
          <div className="storage-details">
            <div className="storage-type">
              <span className="storage-color documents"></span>
              <span>Documents {storageStats.documents.toFixed(2)} GB</span>
            </div>
            <div className="storage-type">
              <span className="storage-color media"></span>
              <span>Media {storageStats.media.toFixed(2)} GB</span>
            </div>
            <div className="storage-type">
              <span className="storage-color other"></span>
              <span>Others {storageStats.other.toFixed(2)} GB</span>
            </div>
          </div>

          <div className="gcp-info">
            <div className="gcp-badge">
              <i className="fa-solid fa-cloud"></i>
              <span>GCP Cloud Storage</span>
            </div>
            <div className="bucket-info">{userData.gcpBucket}</div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="main-content">
        {/* Header */}
        <div className="dashboard-header">
          <div className={`search-bar ${filteredFiles.length > 0 && searchQuery ? "has-results" : ""}`}>
            <i className="fa-solid fa-search"></i>
            <input
              type="text"
              placeholder="Search files..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="header-actions">
            <div className="view-options">
              <button
                className={`view-btn ${viewMode === "grid" ? "active" : ""}`}
                onClick={() => setViewMode("grid")}
                title="Grid View"
              >
                <i className="fa-solid fa-grip"></i>
              </button>
              <button
                className={`view-btn ${viewMode === "list" ? "active" : ""}`}
                onClick={() => setViewMode("list")}
                title="List View"
              >
                <i className="fa-solid fa-list"></i>
              </button>
            </div>
            <button className="upload-btn" onClick={() => setShowUploadModal(true)}>
              <i className="fa-solid fa-cloud-arrow-up"></i>
              <span>Upload</span>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="dashboard-content">
          <div className="content-header">
            <h1>
              {activeTab === "all" && "All Files"}
              {activeTab === "recent" && "Recent Files"}
              {activeTab === "starred" && "Starred Files"}
              {activeTab === "trash" && "Trash"}
            </h1>
            {filteredFiles.length > 0 && <span className="file-count">{filteredFiles.length} files</span>}
          </div>

          {filteredFiles.length > 0 ? (
            <div className={`files-grid ${viewMode === "list" ? "files-list" : ""}`}>
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
                      title={file.starred ? "Remove from starred" : "Add to starred"}
                    >
                      <i className={`fa-${file.starred ? "solid" : "regular"} fa-star`}></i>
                    </button>
                    <button className="action-btn" onClick={() => handleFilePreview(file)} title="Preview file">
                      <i className="fa-solid fa-eye"></i>
                    </button>
                    <button
                      className="action-btn"
                      onClick={() => handleFileDownload(file)}
                      title="Download file"
                      disabled={isDownloading[file.id]}
                    >
                      {isDownloading[file.id] ? (
                        <i className="fa-solid fa-spinner fa-spin"></i>
                      ) : (
                        <i className="fa-solid fa-download"></i>
                      )}
                    </button>
                    <button className="action-btn danger" onClick={() => handleFileDelete(file.id)} title="Delete file">
                      <i className="fa-solid fa-trash"></i>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <i className={emptyState.icon}></i>
              <h2>{emptyState.title}</h2>
              <p>{emptyState.description}</p>
              {emptyState.showUploadButton && (
                <button className="upload-btn-large" onClick={() => setShowUploadModal(true)}>
                  <i className="fa-solid fa-cloud-arrow-up"></i>
                  <span>Upload Files</span>
                </button>
              )}
            </div>
          )}
        </div>

        {/* Upload Modal */}
        {showUploadModal && (
          <div className="upload-modal-overlay">
            <div className="upload-modal">
              <div className="upload-modal-header">
                <h2>Upload to Cloud Storage</h2>
                <button
                  className="close-btn"
                  onClick={() => {
                    if (!isUploading) {
                      setShowUploadModal(false)
                      setUploadProgress(0)
                    }
                  }}
                >
                  <i className="fa-solid fa-times"></i>
                </button>
              </div>

              <div className="upload-modal-content">
                <div className="upload-info">
                  <i className="fa-solid fa-cloud-arrow-up"></i>
                  <p>Upload files securely to your cloud storage</p>
                </div>

                {isUploading ? (
                  <div className="upload-progress">
                    <div className="progress-info">
                      <span>Uploading...</span>
                      <span>{uploadProgress}%</span>
                    </div>
                    <div className="progress-bar">
                      <div className="progress-fill" style={{ width: `${uploadProgress}%` }}></div>
                    </div>
                  </div>
                ) : (
                  <div className={`upload-dropzone ${dragActive ? "drag-active" : ""}`}>
                    <input type="file" id="file-upload" onChange={handleFileUpload} ref={fileInputRef} />
                    <label htmlFor="file-upload">
                      <i className="fa-solid fa-file-arrow-up"></i>
                      <p>Choose a file or drag it here</p>
                      <small>Maximum file size: 100MB</small>
                    </label>
                  </div>
                )}
              </div>

              <div className="upload-modal-footer">
                <button
                  className="cancel-btn"
                  onClick={() => {
                    if (!isUploading) {
                      setShowUploadModal(false)
                      setUploadProgress(0)
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

        {/* Preview Modal */}
        {showPreviewModal && previewFile && (
          <div className="preview-modal-overlay">
            <div className="preview-modal">
              <div className="preview-modal-header">
                <h2>{previewFile.originalName}</h2>
                <div className="preview-actions">
                  <button className="action-btn" onClick={() => handleFileDownload(previewFile)} title="Download">
                    <i className="fa-solid fa-download"></i>
                  </button>
                  <button className="close-btn" onClick={() => setShowPreviewModal(false)}>
                    <i className="fa-solid fa-times"></i>
                  </button>
                </div>
              </div>
              <div className="preview-modal-content">
                {previewFile.contentType?.startsWith("image/") && (
                  <img src={previewUrl || "/placeholder.svg"} alt={previewFile.originalName} />
                )}
                {previewFile.contentType?.startsWith("video/") && <video controls src={previewUrl} />}
                {previewFile.contentType?.startsWith("audio/") && <audio controls src={previewUrl} />}
                {previewFile.contentType === "application/pdf" && (
                  <iframe src={previewUrl} title={previewFile.originalName} />
                )}
                {!previewFile.contentType?.match(/^(image|video|audio)\//) &&
                  previewFile.contentType !== "application/pdf" && (
                    <div className="preview-unavailable">
                      <i className="fa-solid fa-file"></i>
                      <p>Preview not available for this file type</p>
                      <button className="download-btn" onClick={() => handleFileDownload(previewFile)}>
                        <i className="fa-solid fa-download"></i>
                        Download to view
                      </button>
                    </div>
                  )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Dashboard

