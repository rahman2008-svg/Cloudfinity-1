package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isFolder: Boolean,
    val parentFolderId: Long, // 0 for root folder
    val size: Long, // 0 for folder, in bytes
    val mimeType: String, // "directory", "image/jpeg", "video/mp4", "audio/mpeg", "application/pdf", "text/plain"
    val createdTime: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTime: Long = 0,
    val isShared: Boolean = false,
    val shareLink: String? = null,
    val isInVault: Boolean = false,
    val localUri: String? = null, // Path to real image or simulation details
    val videoDuration: Long = 0 // Duration in milliseconds if audio/video
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
    
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
    
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/") || mimeType == "audio/mpeg" || mimeType == "audio/mp3"
    
    val isDocument: Boolean
        get() = mimeType == "application/pdf" || mimeType == "text/plain" || mimeType.contains("word") || mimeType.contains("excel")
}

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val isUpload: Boolean, // true = Upload, false = Download
    val totalSize: Long,
    val progress: Float, // 0.0 to 1.0
    val status: String, // "PENDING", "TRANSFERRING", "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_config")
data class VaultConfigEntity(
    @PrimaryKey val id: Int = 1,
    val vaultPin: String // Empty string means vault PIN is not set yet
)
