package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FileEntity
import com.example.ui.theme.CloudPrimary
import com.example.ui.viewmodel.CloudfinityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesManagerScreen(
    viewModel: CloudfinityViewModel,
    initialCategoryFilter: String = ""
) {
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val folderStack by viewModel.folderStack.collectAsState()
    val currentFiles by viewModel.currentFiles.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Filter categories (by photo, video, music, document, or none)
    var activeCategoryFilter by remember { mutableStateOf(initialCategoryFilter) }

    // Dialog trigger states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showUploadSimulationDialog by remember { mutableStateOf(false) }
    var showAddMenuDialog by remember { mutableStateOf(false) }
    var fileForMenu by remember { mutableStateOf<FileEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val contentResolver = context.contentResolver
    val realFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                var name = "device_file_${System.currentTimeMillis()}"
                var size = 0L
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = it.getString(nameIndex)
                        }
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = it.getLong(sizeIndex)
                        }
                    }
                }
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                viewModel.uploadRealFile(name, size, mimeType, uri.toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Apply active filters on top of directory listing
    val displayedFiles = remember(currentFiles, activeCategoryFilter, searchQuery, searchResults) {
        if (searchQuery.isNotBlank()) {
            searchResults
        } else {
            val baseList = currentFiles
            if (activeCategoryFilter.isBlank()) {
                baseList
            } else {
                baseList.filter { file ->
                    !file.isFolder && when (activeCategoryFilter.lowercase()) {
                        "photo" -> file.isImage
                        "video" -> file.isVideo
                        "music" -> file.isAudio
                        "document" -> file.isDocument
                        else -> true
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (searchQuery.isBlank()) {
                FloatingActionButton(
                    onClick = { showAddMenuDialog = true },
                    containerColor = CloudPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Content", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search files & folders...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CloudPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // 2. Navigation path & category filters
            if (searchQuery.isBlank()) {
                // Folder Breadcrumbs Path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud",
                        tint = CloudPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        folderStack.forEachIndexed { index, pair ->
                            if (index > 0) {
                                Text(" › ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                            }
                            Text(
                                text = pair.second,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (index == folderStack.size - 1) CloudPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.clickable {
                                    // Navigate up to that folder
                                    val countToPop = folderStack.size - 1 - index
                                    for (i in 0 until countToPop) {
                                        viewModel.navigateBack()
                                    }
                                }
                            )
                        }
                    }

                    if (folderStack.size > 1) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Quick horizontal category toggle buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip(name = "All", isSelected = activeCategoryFilter.isBlank()) {
                        activeCategoryFilter = ""
                    }
                    CategoryFilterChip(name = "Photos", isSelected = activeCategoryFilter == "photo") {
                        activeCategoryFilter = "photo"
                    }
                    CategoryFilterChip(name = "Videos", isSelected = activeCategoryFilter == "video") {
                        activeCategoryFilter = "video"
                    }
                    CategoryFilterChip(name = "Music", isSelected = activeCategoryFilter == "music") {
                        activeCategoryFilter = "music"
                    }
                    CategoryFilterChip(name = "Docs", isSelected = activeCategoryFilter == "document") {
                        activeCategoryFilter = "document"
                    }
                }
            } else {
                // Search Result Header
                Text(
                    text = "Search results for '$searchQuery'",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 3. File Explorer Listing
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files or folders found here",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button to upload simulated assets!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayedFiles) { file ->
                        FileListItemRow(
                            file = file,
                            onFolderClick = {
                                viewModel.navigateToFolder(file.id, file.name)
                            },
                            onFileClick = {
                                if (file.isVideo || file.isAudio) {
                                    viewModel.openMediaPlayer(file)
                                } else {
                                    showDetailsDialog = true
                                    fileForMenu = file
                                }
                            },
                            onMenuClick = {
                                fileForMenu = file
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Action Sheet / Bottom Menu for File Options ---
    if (fileForMenu != null) {
        val file = fileForMenu!!
        ModalBottomSheet(
            onDismissRequest = { fileForMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // File Header Info
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileIconWidget(file = file, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (file.isFolder) "Folder" else formatSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Actions Menu
                if (!file.isFolder) {
                    // Download
                    DropdownMenuItem(
                        text = { Text("Download File (Add to Transfers)") },
                        onClick = {
                            viewModel.simulateFileDownload(file)
                            fileForMenu = null
                        },
                        leadingIcon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = CloudPrimary) }
                    )
                }

                // Share
                DropdownMenuItem(
                    text = { Text(if (file.isShared) "Get Shared Link" else "Share & Generate Link") },
                    onClick = {
                        viewModel.shareFile(file.id, file.name)
                        // Trigger copy
                        val link = "https://cloudfinity.net/share/${file.id}"
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        fileForMenu = null
                    },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = Color(0xFF06B6D4)) }
                )

                // Rename
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showRenameDialog = true
                    },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFFF59E0B)) }
                )

                // Move to Secure Vault
                DropdownMenuItem(
                    text = { Text("Move to Secure Vault") },
                    onClick = {
                        viewModel.moveFileToVault(file.id, file.name)
                        fileForMenu = null
                    },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFEF4444)) }
                )

                // Details
                DropdownMenuItem(
                    text = { Text("Details & Properties") },
                    onClick = {
                        showDetailsDialog = true
                    },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Delete
                DropdownMenuItem(
                    text = { Text("Move to Recycle Bin", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        viewModel.softDeleteFile(file.id, file.name)
                        fileForMenu = null
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }

    // --- Create Folder Dialog ---
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Enter folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolder(folderName)
                        showCreateFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloudPrimary)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Rename File/Folder Dialog ---
    if (showRenameDialog && fileForMenu != null) {
        val file = fileForMenu!!
        var renameText by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename item", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameFile(file.id, renameText)
                        showRenameDialog = false
                        fileForMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloudPrimary)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Details & Properties Dialog ---
    if (showDetailsDialog && fileForMenu != null) {
        val file = fileForMenu!!
        val formatter = remember { SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()) }
        val dateString = formatter.format(Date(file.createdTime))
        
        AlertDialog(
            onDismissRequest = { 
                showDetailsDialog = false
                fileForMenu = null
            },
            title = { Text("Item Properties", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PropertyField(label = "Name", value = file.name)
                    PropertyField(label = "Type", value = if (file.isFolder) "Folder" else file.mimeType)
                    if (!file.isFolder) {
                        PropertyField(label = "Size", value = formatSize(file.size))
                    }
                    PropertyField(label = "Created", value = dateString)
                    PropertyField(label = "Cloud Safe", value = if (file.isInVault) "Protected" else "Public")
                    PropertyField(label = "Shared Status", value = if (file.isShared) "Active Shared Link" else "Unshared")
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showDetailsDialog = false
                        fileForMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloudPrimary)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // --- Upload Simulation Dialog (THE BEST FEATURE FOR REAL INTERACTIVE DEMO) ---
    if (showUploadSimulationDialog) {
        var uploadName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("photo") }
        var simulatedSizeMB by remember { mutableStateOf(12f) }

        AlertDialog(
            onDismissRequest = { showUploadSimulationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CloudPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Cloud Upload", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Because Cloudfinity is client-only (no cloud API charges!), you can simulate real-time background file uploads of any size.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // File Category Selection
                    Column {
                        Text("1. File Category:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UploadTypeButton(name = "Photo", isSelected = selectedType == "photo") {
                                selectedType = "photo"
                                if (uploadName.isBlank() || uploadName.startsWith("My ") || uploadName.startsWith("Drone ") || uploadName.startsWith("Track ") || uploadName.startsWith("Document ")) {
                                    uploadName = "My Snapshot ${System.currentTimeMillis().toString().takeLast(4)}.jpg"
                                }
                            }
                            UploadTypeButton(name = "Video", isSelected = selectedType == "video") {
                                selectedType = "video"
                                if (uploadName.isBlank() || uploadName.startsWith("My ") || uploadName.startsWith("Drone ") || uploadName.startsWith("Track ") || uploadName.startsWith("Document ")) {
                                    uploadName = "Drone Clip ${System.currentTimeMillis().toString().takeLast(4)}.mp4"
                                }
                            }
                            UploadTypeButton(name = "Music", isSelected = selectedType == "music") {
                                selectedType = "music"
                                if (uploadName.isBlank() || uploadName.startsWith("My ") || uploadName.startsWith("Drone ") || uploadName.startsWith("Track ") || uploadName.startsWith("Document ")) {
                                    uploadName = "Track Beat ${System.currentTimeMillis().toString().takeLast(4)}.mp3"
                                }
                            }
                            UploadTypeButton(name = "Doc", isSelected = selectedType == "document") {
                                selectedType = "document"
                                if (uploadName.isBlank() || uploadName.startsWith("My ") || uploadName.startsWith("Drone ") || uploadName.startsWith("Track ") || uploadName.startsWith("Document ")) {
                                    uploadName = "Document Contract ${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                                }
                            }
                        }
                    }

                    // Prepopulate name initially if blank
                    LaunchedEffect(selectedType) {
                        if (uploadName.isBlank()) {
                            uploadName = "My Snapshot ${System.currentTimeMillis().toString().takeLast(4)}.jpg"
                        }
                    }

                    // Name Input
                    OutlinedTextField(
                        value = uploadName,
                        onValueChange = { uploadName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("2. Simulated Size:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = String.format(Locale.US, "%.1f MB", simulatedSizeMB),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = CloudPrimary)
                            )
                        }
                        Slider(
                            value = simulatedSizeMB,
                            onValueChange = { simulatedSizeMB = it },
                            valueRange = 1f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = CloudPrimary,
                                activeTrackColor = CloudPrimary
                            )
                        )
                    }

                    // Directory creation alternative
                    TextButton(
                        onClick = {
                            showCreateFolderDialog = true
                            showUploadSimulationDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Folder Instead", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uploadName.isNotBlank()) {
                            viewModel.simulateFileUpload(uploadName, simulatedSizeMB, selectedType)
                            showUploadSimulationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloudPrimary)
                ) {
                    Text("Start Upload", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadSimulationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddMenuDialog) {
        AlertDialog(
            onDismissRequest = { showAddMenuDialog = false },
            title = { Text("Choose Action", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "You can upload real files directly from your mobile device or simulate a cloud upload process.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Option 1: Pick real file
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                realFilePickerLauncher.launch("*/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = CloudPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Import Real File", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Pick photos, videos, or files from your phone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Simulate cloud upload
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                showUploadSimulationDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Simulate Cloud Upload", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Represent a fast progressive mock background transfer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 3: Create Folder
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                showCreateFolderDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Create Folder", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Create a nested subdirectory in Cloudfinity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMenuDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CategoryFilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CloudPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun RowScope.UploadTypeButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, if (isSelected) CloudPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = if (isSelected) CloudPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) CloudPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun FileListItemRow(
    file: FileEntity,
    onFolderClick: () -> Unit,
    onFileClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(file.createdTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (file.isFolder) onFolderClick() else onFileClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail/Icon
        FileIconWidget(file = file, modifier = Modifier.size(44.dp))

        Spacer(modifier = Modifier.width(14.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (file.isFolder) "Folder • $formattedDate" else "${formatSize(file.size)} • $formattedDate",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // Quick Actions indicators
        if (file.isVideo || file.isAudio) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Playable",
                tint = CloudPrimary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
        }

        // Action menu
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FileIconWidget(file: FileEntity, modifier: Modifier = Modifier) {
    if (file.isImage && !file.localUri.isNullOrEmpty()) {
        AsyncImage(
            model = file.localUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
        )
    } else {
        val icon = when {
            file.isFolder -> Icons.Default.Folder
            file.isImage -> Icons.Default.Image
            file.isVideo -> Icons.Default.VideoFile
            file.isAudio -> Icons.Default.Audiotrack
            file.isDocument -> Icons.Default.Description
            else -> Icons.Default.InsertDriveFile
        }

        val iconColor = when {
            file.isFolder -> Color(0xFFF59E0B) // Golden Folder
            file.isImage -> Color(0xFF2563EB)
            file.isVideo -> Color(0xFFEA580C)
            file.isAudio -> Color(0xFF7C3AED)
            file.isDocument -> Color(0xFF059669)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PropertyField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CloudPrimary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
