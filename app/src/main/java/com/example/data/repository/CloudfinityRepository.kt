package com.example.data.repository

import com.example.data.local.FileDao
import com.example.data.local.TransferDao
import com.example.data.local.VaultDao
import com.example.data.model.FileEntity
import com.example.data.model.TransferEntity
import com.example.data.model.VaultConfigEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class CloudfinityRepository(
    private val fileDao: FileDao,
    private val transferDao: TransferDao,
    private val vaultDao: VaultDao
) {
    // Files Streams
    fun getFilesByParent(parentId: Long): Flow<List<FileEntity>> =
        fileDao.getFilesByParent(parentId)

    fun getFilesByParentAndVault(parentId: Long, inVault: Boolean): Flow<List<FileEntity>> =
        fileDao.getFilesByParentAndVault(parentId, inVault)

    val deletedFiles: Flow<List<FileEntity>> = fileDao.getDeletedFiles()
    val vaultFiles: Flow<List<FileEntity>> = fileDao.getVaultFiles()
    val sharedFiles: Flow<List<FileEntity>> = fileDao.getSharedFiles()
    
    fun searchFiles(query: String): Flow<List<FileEntity>> =
        fileDao.searchFiles(query)

    // Direct Operations
    suspend fun getFileById(id: Long): FileEntity? = fileDao.getFileById(id)

    suspend fun createFolder(name: String, parentFolderId: Long, isInVault: Boolean = false): Long {
        val folder = FileEntity(
            name = name,
            isFolder = true,
            parentFolderId = parentFolderId,
            size = 0,
            mimeType = "directory",
            isInVault = isInVault
        )
        return fileDao.insertFile(folder)
    }

    suspend fun insertFileDirect(file: FileEntity): Long {
        return fileDao.insertFile(file)
    }

    suspend fun renameFile(id: Long, newName: String) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            val extension = if (file.isFolder) "" else getExtension(file.name)
            val nameWithExt = if (extension.isNotEmpty() && !newName.endsWith(extension)) {
                "$newName.$extension"
            } else {
                newName
            }
            fileDao.updateFile(file.copy(name = nameWithExt))
        }
    }

    private fun getExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < filename.length - 1) {
            filename.substring(lastDot + 1)
        } else {
            ""
        }
    }

    suspend fun softDeleteFile(id: Long) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            fileDao.updateFile(
                file.copy(
                    isDeleted = true,
                    deletedTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun restoreFile(id: Long) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            fileDao.updateFile(
                file.copy(
                    isDeleted = false,
                    deletedTime = 0
                )
            )
        }
    }

    suspend fun deleteFilePermanent(id: Long) {
        fileDao.deleteFileById(id)
    }

    suspend fun emptyRecycleBin() {
        fileDao.emptyRecycleBin()
    }

    suspend fun moveFileToVault(id: Long) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            fileDao.updateFile(file.copy(isInVault = true))
        }
    }

    suspend fun extractFileFromVault(id: Long, targetParentId: Long) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            fileDao.updateFile(
                file.copy(
                    isInVault = false,
                    parentFolderId = targetParentId
                )
            )
        }
    }

    suspend fun shareFile(id: Long): String {
        val file = fileDao.getFileById(id)
        if (file != null) {
            val uniqueLink = "https://cloudfinity.net/share/${UUID.randomUUID().toString().take(8)}"
            fileDao.updateFile(
                file.copy(
                    isShared = true,
                    shareLink = uniqueLink
                )
            )
            return uniqueLink
        }
        return ""
    }

    suspend fun unshareFile(id: Long) {
        val file = fileDao.getFileById(id)
        if (file != null) {
            fileDao.updateFile(
                file.copy(
                    isShared = false,
                    shareLink = null
                )
            )
        }
    }

    // Vault PIN Operations
    fun getVaultConfigFlow(): Flow<VaultConfigEntity?> = vaultDao.getVaultConfigFlow()
    
    suspend fun isVaultPinSet(): Boolean {
        val config = vaultDao.getVaultConfigDirect()
        return config != null && config.vaultPin.isNotEmpty()
    }

    suspend fun verifyVaultPin(pin: String): Boolean {
        val config = vaultDao.getVaultConfigDirect()
        return config != null && config.vaultPin == pin
    }

    suspend fun setVaultPin(pin: String) {
        vaultDao.insertVaultConfig(VaultConfigEntity(vaultPin = pin))
    }

    // Transfers streams
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()
    val activeTransfers: Flow<List<TransferEntity>> = transferDao.getActiveTransfers()

    suspend fun clearCompletedTransfers() {
        transferDao.clearCompletedTransfers()
    }

    // Simulates an upload transfer and then writes file to DB
    suspend fun startSimulatedUpload(
        name: String,
        size: Long,
        mimeType: String,
        parentFolderId: Long,
        isInVault: Boolean = false,
        localUri: String? = null,
        duration: Long = 0
    ) {
        // 1. Create a transfer entry
        val transferId = transferDao.insertTransfer(
            TransferEntity(
                fileName = name,
                isUpload = true,
                totalSize = size,
                progress = 0.0f,
                status = "PENDING"
            )
        )

        // Simulating progressive transfer
        delay(600)
        var currentProgress = 0.0f
        transferDao.updateTransfer(
            TransferEntity(
                id = transferId,
                fileName = name,
                isUpload = true,
                totalSize = size,
                progress = currentProgress,
                status = "TRANSFERRING"
            )
        )

        val steps = 8
        for (i in 1..steps) {
            delay(500)
            currentProgress = i.toFloat() / steps.toFloat()
            transferDao.updateTransfer(
                TransferEntity(
                    id = transferId,
                    fileName = name,
                    isUpload = true,
                    totalSize = size,
                    progress = currentProgress,
                    status = "TRANSFERRING"
                )
            )
        }

        // Complete transfer
        transferDao.updateTransfer(
            TransferEntity(
                id = transferId,
                fileName = name,
                isUpload = true,
                totalSize = size,
                progress = 1.0f,
                status = "COMPLETED"
            )
        )

        // 2. Add actual file entity to database
        fileDao.insertFile(
            FileEntity(
                name = name,
                isFolder = false,
                parentFolderId = parentFolderId,
                size = size,
                mimeType = mimeType,
                isInVault = isInVault,
                localUri = localUri,
                videoDuration = duration
            )
        )
    }

    // Simulates a download transfer
    suspend fun startSimulatedDownload(file: FileEntity) {
        val transferId = transferDao.insertTransfer(
            TransferEntity(
                fileName = file.name,
                isUpload = false,
                totalSize = file.size,
                progress = 0.0f,
                status = "PENDING"
            )
        )

        delay(500)
        var currentProgress = 0.0f
        transferDao.updateTransfer(
            TransferEntity(
                id = transferId,
                fileName = file.name,
                isUpload = false,
                totalSize = file.size,
                progress = currentProgress,
                status = "TRANSFERRING"
            )
        )

        val steps = 5
        for (i in 1..steps) {
            delay(400)
            currentProgress = i.toFloat() / steps.toFloat()
            transferDao.updateTransfer(
                TransferEntity(
                    id = transferId,
                    fileName = file.name,
                    isUpload = false,
                    totalSize = file.size,
                    progress = currentProgress,
                    status = "TRANSFERRING"
                )
            )
        }

        transferDao.updateTransfer(
            TransferEntity(
                id = transferId,
                fileName = file.name,
                isUpload = false,
                totalSize = file.size,
                progress = 1.0f,
                status = "COMPLETED"
            )
        )
    }

    // Insert dummy files initially to make the app ready with 120GB used and nice folder structures
    suspend fun prepopulateDatabaseIfEmpty() {
        // Check if database is empty
        val existing = fileDao.getAllActiveFiles().first()
        if (existing.isEmpty()) {
            // Setup Vault PIN default config
            if (vaultDao.getVaultConfigDirect() == null) {
                vaultDao.insertVaultConfig(VaultConfigEntity(vaultPin = ""))
            }

            // Create default folders in root (ID 0)
            val folderPhotosId = fileDao.insertFile(
                FileEntity(name = "My Photos", isFolder = true, parentFolderId = 0, size = 0, mimeType = "directory")
            )
            val folderVideosId = fileDao.insertFile(
                FileEntity(name = "Cinematic Drone Clips", isFolder = true, parentFolderId = 0, size = 0, mimeType = "directory")
            )
            val folderDocsId = fileDao.insertFile(
                FileEntity(name = "Business & Projects", isFolder = true, parentFolderId = 0, size = 0, mimeType = "directory")
            )
            val folderMusicId = fileDao.insertFile(
                FileEntity(name = "Favorite Soundtracks", isFolder = true, parentFolderId = 0, size = 0, mimeType = "directory")
            )

            // Add dummy files into folders to simulate initial usage of ~120 GB
            // Photos (Image files)
            fileDao.insertFiles(
                listOf(
                    FileEntity(
                        name = "Summer Beach Sunset.jpg",
                        isFolder = false,
                        parentFolderId = folderPhotosId,
                        size = 4_500_000L, // 4.5 MB
                        mimeType = "image/jpeg",
                        localUri = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80"
                    ),
                    FileEntity(
                        name = "Mountain Hiking Peaks.jpg",
                        isFolder = false,
                        parentFolderId = folderPhotosId,
                        size = 3_800_000L, // 3.8 MB
                        mimeType = "image/jpeg",
                        localUri = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80"
                    ),
                    FileEntity(
                        name = "Neon Tokyo Streets.jpg",
                        isFolder = false,
                        parentFolderId = folderPhotosId,
                        size = 5_200_000L, // 5.2 MB
                        mimeType = "image/jpeg",
                        localUri = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=600&q=80"
                    )
                )
            )

            // Videos (Video files)
            fileDao.insertFiles(
                listOf(
                    FileEntity(
                        name = "Ocean Waves Loop.mp4",
                        isFolder = false,
                        parentFolderId = folderVideosId,
                        size = 450_000_000L, // 450 MB
                        mimeType = "video/mp4",
                        videoDuration = 32_000L // 32 seconds
                    ),
                    FileEntity(
                        name = "Forest Stream Relaxation.mp4",
                        isFolder = false,
                        parentFolderId = folderVideosId,
                        size = 850_000_000L, // 850 MB
                        mimeType = "video/mp4",
                        videoDuration = 120_000L // 2 mins
                    ),
                    FileEntity(
                        name = "Cyberpunk Animated Intro.mp4",
                        isFolder = false,
                        parentFolderId = folderVideosId,
                        size = 120_000_000L, // 120 MB
                        mimeType = "video/mp4",
                        videoDuration = 15_000L // 15 seconds
                    )
                )
            )

            // Documents (Doc files)
            fileDao.insertFiles(
                listOf(
                    FileEntity(
                        name = "NexVora Project Blueprint.pdf",
                        isFolder = false,
                        parentFolderId = folderDocsId,
                        size = 12_400_000L, // 12.4 MB
                        mimeType = "application/pdf"
                    ),
                    FileEntity(
                        name = "Independent Developer Pitch.txt",
                        isFolder = false,
                        parentFolderId = folderDocsId,
                        size = 150_000L, // 150 KB
                        mimeType = "text/plain"
                    ),
                    FileEntity(
                        name = "Cloudfinity Annual Plan.xlsx",
                        isFolder = false,
                        parentFolderId = folderDocsId,
                        size = 850_000L, // 850 KB
                        mimeType = "application/vnd.ms-excel"
                    )
                )
            )

            // Music (Audio files)
            fileDao.insertFiles(
                listOf(
                    FileEntity(
                        name = "Synthwave Neon Horizon.mp3",
                        isFolder = false,
                        parentFolderId = folderMusicId,
                        size = 8_200_000L, // 8.2 MB
                        mimeType = "audio/mpeg",
                        videoDuration = 245_000L // 4m 5s
                    ),
                    FileEntity(
                        name = "Acoustic Sunset Breeze.mp3",
                        isFolder = false,
                        parentFolderId = folderMusicId,
                        size = 6_100_000L, // 6.1 MB
                        mimeType = "audio/mpeg",
                        videoDuration = 182_000L // 3m 2s
                    )
                )
            )

            // Add a few loose files in root (0)
            fileDao.insertFile(
                FileEntity(
                    name = "Quick Guide - Start Here.pdf",
                    isFolder = false,
                    parentFolderId = 0,
                    size = 1_200_000L, // 1.2 MB
                    mimeType = "application/pdf"
                )
            )
            fileDao.insertFile(
                FileEntity(
                    name = "Cloudfinity Welcome Banner.jpg",
                    isFolder = false,
                    parentFolderId = 0,
                    size = 2_100_000L, // 2.1 MB
                    mimeType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1544396821-4dd40b938ad3?auto=format&fit=crop&w=600&q=80"
                )
            )
        }
    }
}
