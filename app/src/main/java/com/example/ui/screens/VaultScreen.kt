package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntity
import com.example.ui.theme.CloudCyan
import com.example.ui.theme.CloudPrimary
import com.example.ui.viewmodel.CloudfinityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: CloudfinityViewModel) {
    val vaultConfig by viewModel.vaultConfig.collectAsState()
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    val context = LocalContext.current

    // Decide what sub-screen to render
    val hasPinSet = vaultConfig != null && vaultConfig!!.vaultPin.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasPinSet) {
            // Screen A: Initial PIN Configuration Setup
            VaultPinSetupScreen(onSetupSuccess = { pin ->
                viewModel.setupVaultPin(pin)
            })
        } else if (!isUnlocked) {
            // Screen B: PIN Unlock Keypad Lockscreen
            VaultLockscreen(
                onUnlockAttempt = { enteredPin ->
                    viewModel.unlockVault(enteredPin)
                }
            )
        } else {
            // Screen C: Secure Vault Workspace (File listing)
            VaultWorkspaceScreen(
                vaultFiles = vaultFiles,
                onLockClick = { viewModel.lockVault() },
                onExtractClick = { file ->
                    viewModel.extractFileFromVault(file.id, file.name)
                },
                onDeletePermanentClick = { file ->
                    viewModel.deleteFilePermanent(file.id, file.name)
                }
            )
        }
    }
}

@Composable
fun VaultPinSetupScreen(onSetupSuccess: (String) -> Unit) {
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CloudPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CloudPrimary, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (!isConfirmStep) "Setup Personal Vault" else "Confirm Secure PIN",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (!isConfirmStep) 
                "Create a 4-digit PIN to secure your private documents, videos, and images."
                else "Re-enter your 4-digit PIN to verify and activate the vault.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Dots Display
        val activePin = if (!isConfirmStep) pinText else confirmPinText
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < activePin.length) CloudPrimary 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Custom Security Keypad Grid
        KeypadGrid(
            onNumberClick = { num ->
                if (activePin.length < 4) {
                    val updated = activePin + num
                    if (!isConfirmStep) {
                        pinText = updated
                        if (updated.length == 4) {
                            // Proceed to confirm step
                            isConfirmStep = true
                        }
                    } else {
                        confirmPinText = updated
                        if (updated.length == 4) {
                            // Check matching
                            if (pinText == confirmPinText) {
                                onSetupSuccess(pinText)
                            } else {
                                Toast.makeText(context, "PIN mismatch, starting over", Toast.LENGTH_SHORT).show()
                                pinText = ""
                                confirmPinText = ""
                                isConfirmStep = false
                            }
                        }
                    }
                }
            },
            onClearClick = {
                if (!isConfirmStep) {
                    if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                } else {
                    if (confirmPinText.isNotEmpty()) confirmPinText = confirmPinText.dropLast(1)
                }
            }
        )
    }
}

@Composable
fun VaultLockscreen(onUnlockAttempt: (String) -> Boolean) {
    var enteredPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vault Locked",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter your 4-digit PIN to access private files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Dots Display
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < enteredPin.length) Color(0xFFEF4444) 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Keypad
        KeypadGrid(
            onNumberClick = { num ->
                if (enteredPin.length < 4) {
                    val updated = enteredPin + num
                    enteredPin = updated
                    if (updated.length == 4) {
                        onUnlockAttempt(updated)
                        enteredPin = "" // Clear for next time
                    }
                }
            },
            onClearClick = {
                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultWorkspaceScreen(
    vaultFiles: List<FileEntity>,
    onLockClick: () -> Unit,
    onExtractClick: (FileEntity) -> Unit,
    onDeletePermanentClick: (FileEntity) -> Unit
) {
    var fileForAction by remember { mutableStateOf<FileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = CloudCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Secure Vault", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onLockClick) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock Safe", tint = Color(0xFFEF4444))
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(colors = listOf(Color(0xFFEF4444).copy(alpha = 0.8f), Color(0xFFFF8A8A).copy(alpha = 0.8f)))
                    )
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Files in the vault are hidden from search and recent files list. Extract them to restore access in public lists.",
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                }
            }

            if (vaultFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderZip,
                            contentDescription = "Empty Safe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Safe Vault is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To secure files, go to Files, open a file's action menu, and choose 'Move to Secure Vault'.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vaultFiles) { file ->
                        FileListItemRow(
                            file = file,
                            onFolderClick = {},
                            onFileClick = {
                                fileForAction = file
                            },
                            onMenuClick = {
                                fileForAction = file
                            }
                        )
                    }
                }
            }
        }
    }

    // Vault actions sheet
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
                    text = { Text("Extract from Safe (Restore to Main Workspace)") },
                    onClick = {
                        onExtractClick(file)
                        fileForAction = null
                    },
                    leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null, tint = CloudPrimary) }
                )

                DropdownMenuItem(
                    text = { Text("Delete Permanently from Device", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onDeletePermanentClick(file)
                        fileForAction = null
                    },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

@Composable
fun KeypadGrid(
    onNumberClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "", "0", "C"
    )

    Column(
        modifier = Modifier.width(260.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 3) {
                    val key = keys[row * 3 + col]
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(60.dp))
                    } else if (key == "C") {
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                                .clickable { onNumberClick(key) }
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
