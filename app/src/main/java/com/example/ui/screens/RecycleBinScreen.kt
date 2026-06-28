package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntity
import com.example.ui.theme.CloudPrimary
import com.example.ui.viewmodel.CloudfinityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: CloudfinityViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedFiles by viewModel.deletedFiles.collectAsState()
    var fileForAction by remember { mutableStateOf<FileEntity?>(null) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedFiles.isNotEmpty()) {
                        IconButton(onClick = { showEmptyBinDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty Bin", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Notice banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = CloudPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Items in the Recycle Bin will remain stored locally until emptied or permanently removed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            if (deletedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Empty Trash",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recycle bin is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(deletedFiles) { file ->
                        FileListItemRow(
                            file = file,
                            onFolderClick = {},
                            onFileClick = { fileForAction = file },
                            onMenuClick = { fileForAction = file }
                        )
                    }
                }
            }
        }
    }

    // Recover options bottom sheet
    if (fileForAction != null) {
        val file = fileForAction!!
        ModalBottomSheet(
            onDismissRequest = { fileForAction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                DropdownMenuItem(
                    text = { Text("Restore File") },
                    onClick = {
                        viewModel.restoreFile(file.id, file.name)
                        fileForAction = null
                    },
                    leadingIcon = { Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null, tint = CloudPrimary) }
                )

                DropdownMenuItem(
                    text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        viewModel.deleteFilePermanent(file.id, file.name)
                        fileForAction = null
                    },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }

    // Confirm Empty Bin Dialog
    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            title = { Text("Empty Recycle Bin?", fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone. All files in the recycle bin will be permanently erased from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyRecycleBin()
                        showEmptyBinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
