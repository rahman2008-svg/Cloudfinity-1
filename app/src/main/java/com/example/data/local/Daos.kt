package com.example.data.local

import androidx.room.*
import com.example.data.model.FileEntity
import com.example.data.model.TransferEntity
import com.example.data.model.VaultConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE isDeleted = 0 AND isInVault = 0 ORDER BY isFolder DESC, name ASC")
    fun getAllActiveFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE parentFolderId = :parentId AND isDeleted = 0 AND isInVault = 0 ORDER BY isFolder DESC, name ASC")
    fun getFilesByParent(parentId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE parentFolderId = :parentId AND isDeleted = 0 AND isInVault = :inVault ORDER BY isFolder DESC, name ASC")
    fun getFilesByParentAndVault(parentId: Long, inVault: Boolean): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE isDeleted = 1 ORDER BY deletedTime DESC")
    fun getDeletedFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isInVault = 1 AND isDeleted = 0 ORDER BY isFolder DESC, name ASC")
    fun getVaultFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isShared = 1 AND isDeleted = 0 ORDER BY createdTime DESC")
    fun getSharedFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 AND isInVault = 0 ORDER BY isFolder DESC, name ASC")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM files WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE status IN ('PENDING', 'TRANSFERRING') ORDER BY timestamp ASC")
    fun getActiveTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Update
    suspend fun updateTransfer(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransferById(id: Long)

    @Query("DELETE FROM transfers WHERE status IN ('COMPLETED', 'FAILED')")
    suspend fun clearCompletedTransfers()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_config WHERE id = 1")
    fun getVaultConfigFlow(): Flow<VaultConfigEntity?>

    @Query("SELECT * FROM vault_config WHERE id = 1")
    suspend fun getVaultConfigDirect(): VaultConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultConfig(config: VaultConfigEntity)
}
