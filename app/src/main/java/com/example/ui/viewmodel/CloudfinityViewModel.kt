package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FileEntity
import com.example.data.model.TransferEntity
import com.example.data.model.VaultConfigEntity
import com.example.data.repository.CloudfinityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CloudfinityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CloudfinityRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = CloudfinityRepository(
            database.fileDao(),
            database.transferDao(),
            database.vaultDao()
        )
        // Ensure database has nice initial folders/files on first launch
        viewModelScope.launch {
            repository.prepopulateDatabaseIfEmpty()
        }
    }

    // Navigation Stack
    private val _folderStack = MutableStateFlow<List<Pair<Long, String>>>(listOf(0L to "Cloudfinity"))
    val folderStack: StateFlow<List<Pair<Long, String>>> = _folderStack.asStateFlow()

    val currentFolderId: StateFlow<Long> = _folderStack
        .map { it.last().first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Breadcrumbs String
    val currentBreadcrumbs: StateFlow<String> = _folderStack
        .map { stack -> stack.joinToString(" > ") { it.second } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cloudfinity")

    // Active files in current folder
    val currentFiles: StateFlow<List<FileEntity>> = currentFolderId
        .flatMapLatest { folderId ->
            repository.getFilesByParentAndVault(folderId, inVault = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Query & Stream
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<FileEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchFiles(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deleted Files (Recycle Bin)
    val deletedFiles: StateFlow<List<FileEntity>> = repository.deletedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shared Files
    val sharedFiles: StateFlow<List<FileEntity>> = repository.sharedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Files
    val vaultFiles: StateFlow<List<FileEntity>> = repository.vaultFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Transfers & History
    val allTransfers: StateFlow<List<TransferEntity>> = repository.allTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTransfers: StateFlow<List<TransferEntity>> = repository.activeTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Configuration
    val vaultConfig: StateFlow<VaultConfigEntity?> = repository.getVaultConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Multimedia Player State
    private val _activePlayerFile = MutableStateFlow<FileEntity?>(null)
    val activePlayerFile: StateFlow<FileEntity?> = _activePlayerFile.asStateFlow()

    private val _playerPlaying = MutableStateFlow(false)
    val playerPlaying: StateFlow<Boolean> = _playerPlaying.asStateFlow()

    private val _playerProgress = MutableStateFlow(0.0f)
    val playerProgress: StateFlow<Float> = _playerProgress.asStateFlow()

    private val _playerCurrentTime = MutableStateFlow(0L)
    val playerCurrentTime: StateFlow<Long> = _playerCurrentTime.asStateFlow()

    // Notification State
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Navigation Methods
    fun navigateToFolder(folderId: Long, folderName: String) {
        val current = _folderStack.value.toMutableList()
        current.add(folderId to folderName)
        _folderStack.value = current
    }

    fun navigateBack(): Boolean {
        val current = _folderStack.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            _folderStack.value = current
            return true // handled
        }
        return false // already at root
    }

    fun navigateToRoot() {
        _folderStack.value = listOf(0L to "Cloudfinity")
    }

    // Set Search Query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Folder Actions
    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                showToast("Folder name cannot be empty")
                return@launch
            }
            repository.createFolder(name, currentFolderId.value)
            showToast("Created folder '$name'")
        }
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) {
                showToast("Name cannot be empty")
                return@launch
            }
            repository.renameFile(fileId, newName)
            showToast("Renamed successfully")
        }
    }

    fun softDeleteFile(fileId: Long, fileName: String) {
        viewModelScope.launch {
            repository.softDeleteFile(fileId)
            showToast("'$fileName' moved to Recycle Bin")
        }
    }

    fun restoreFile(fileId: Long, fileName: String) {
        viewModelScope.launch {
            repository.restoreFile(fileId)
            showToast("Restored '$fileName'")
        }
    }

    fun deleteFilePermanent(fileId: Long, fileName: String) {
        viewModelScope.launch {
            repository.deleteFilePermanent(fileId)
            showToast("Permanently deleted '$fileName'")
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
            showToast("Recycle bin emptied")
        }
    }

    // Shared files
    fun shareFile(fileId: Long, fileName: String) {
        viewModelScope.launch {
            val link = repository.shareFile(fileId)
            if (link.isNotEmpty()) {
                showToast("Generated share link for $fileName")
            }
        }
    }

    fun unshareFile(fileId: Long) {
        viewModelScope.launch {
            repository.unshareFile(fileId)
            showToast("File sharing stopped")
        }
    }

    // Vault Security Actions
    fun setupVaultPin(pin: String) {
        viewModelScope.launch {
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                showToast("PIN must be exactly 4 digits")
                return@launch
            }
            repository.setVaultPin(pin)
            _isVaultUnlocked.value = true
            showToast("Vault Secure PIN set successfully")
        }
    }

    fun unlockVault(pin: String): Boolean {
        var success = false
        viewModelScope.launch {
            if (repository.verifyVaultPin(pin)) {
                _isVaultUnlocked.value = true
                success = true
                showToast("Vault Unlocked")
            } else {
                showToast("Incorrect Secure PIN")
            }
        }
        return success
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        showToast("Vault Locked")
    }

    fun moveFileToVault(fileId: Long, fileName: String) {
        viewModelScope.launch {
            repository.moveFileToVault(fileId)
            showToast("Moved '$fileName' to Secure Vault")
        }
    }

    fun extractFileFromVault(fileId: Long, fileName: String) {
        viewModelScope.launch {
            // Extracts to current folder in main workspace
            repository.extractFileFromVault(fileId, currentFolderId.value)
            showToast("Extracted '$fileName' from Vault")
        }
    }

    // Simulation Upload
    fun simulateFileUpload(name: String, sizeInMB: Float, type: String) {
        viewModelScope.launch {
            val sizeBytes = (sizeInMB * 1024 * 1024).toLong()
            val mimeType = when (type.lowercase()) {
                "photo" -> "image/jpeg"
                "video" -> "video/mp4"
                "music" -> "audio/mpeg"
                "document" -> "application/pdf"
                else -> "text/plain"
            }

            // Duration for media files
            val duration = if (type.lowercase() == "video") {
                (10_000L..180_000L).random()
            } else if (type.lowercase() == "music") {
                (120_000L..300_000L).random()
            } else {
                0L
            }

            // Unsplash placeholder URI for simulation
            val localUri = when (type.lowercase()) {
                "photo" -> {
                    val photos = listOf(
                        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=600&q=80"
                    )
                    photos.random()
                }
                else -> null
            }

            showToast("Starting upload: $name")
            repository.startSimulatedUpload(
                name = name,
                size = sizeBytes,
                mimeType = mimeType,
                parentFolderId = currentFolderId.value,
                isInVault = false,
                localUri = localUri,
                duration = duration
            )
            showToast("Upload completed: $name")
        }
    }
    
    // Real File Import
    fun uploadRealFile(name: String, size: Long, mimeType: String, localUri: String) {
        viewModelScope.launch {
            showToast("Importing: $name")
            val duration = if (mimeType.startsWith("video/")) {
                (10_000L..180_000L).random()
            } else if (mimeType.startsWith("audio/")) {
                (120_000L..300_000L).random()
            } else {
                0L
            }
            repository.startSimulatedUpload(
                name = name,
                size = size,
                mimeType = mimeType,
                parentFolderId = currentFolderId.value,
                isInVault = false,
                localUri = localUri,
                duration = duration
            )
            showToast("Successfully imported '$name'")
        }
    }

    // Simulation Download
    fun simulateFileDownload(file: FileEntity) {
        viewModelScope.launch {
            showToast("Added to download queue: ${file.name}")
            repository.startSimulatedDownload(file)
            showToast("Download finished: ${file.name}")
        }
    }

    // Clear completed downloads
    fun clearFinishedTransfers() {
        viewModelScope.launch {
            repository.clearCompletedTransfers()
            showToast("Transfer history cleared")
        }
    }

    // Video Player Simulation Controls
    fun openMediaPlayer(file: FileEntity) {
        _activePlayerFile.value = file
        _playerPlaying.value = true
        _playerProgress.value = 0.0f
        _playerCurrentTime.value = 0L
        
        // Start simple loop to simulate playback progress
        viewModelScope.launch {
            val totalDuration = file.videoDuration
            while (_activePlayerFile.value?.id == file.id) {
                if (_playerPlaying.value) {
                    var currTime = _playerCurrentTime.value + 1000L
                    if (currTime >= totalDuration) {
                        currTime = 0L // Loop around
                    }
                    _playerCurrentTime.value = currTime
                    _playerProgress.value = currTime.toFloat() / totalDuration.toFloat()
                }
                delay(1000L)
            }
        }
    }

    fun togglePlayerPlayPause() {
        _playerPlaying.value = !_playerPlaying.value
    }

    fun seekPlayer(progress: Float) {
        val file = _activePlayerFile.value ?: return
        _playerProgress.value = progress
        _playerCurrentTime.value = (progress * file.videoDuration).toLong()
    }

    fun closeMediaPlayer() {
        _activePlayerFile.value = null
        _playerPlaying.value = false
        _playerProgress.value = 0.0f
        _playerCurrentTime.value = 0L
    }

    // Helper functions
    private fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(2500)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }
}
