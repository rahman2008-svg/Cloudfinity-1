package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.CloudCyan
import com.example.ui.theme.CloudPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CloudfinityViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CloudfinityViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Active tab state (0: Home, 1: Files, 2: Transfers, 3: Vault, 4: Creator)
                var activeTab by remember { mutableStateOf(0) }
                
                // Auxiliary screen overlays
                var showRecycleBinScreen by remember { mutableStateOf(false) }
                var showSharedLinksScreen by remember { mutableStateOf(false) }
                
                // Live category jump filter
                var filesCategoryJump by remember { mutableStateOf("") }

                // Observe toast alerts
                val toastMessage by viewModel.toastMessage.collectAsState()
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            if (!showRecycleBinScreen) {
                                CenterAlignedTopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudQueue,
                                                contentDescription = "Cloudfinity",
                                                tint = CloudPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cloudfinity",
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                fontSize = 20.sp
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        // Left Profile Avatar pointing to Creator Studio Tab (4)
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(CloudPrimary, CloudCyan)
                                                    )
                                                )
                                                .clickable { activeTab = 4 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "AR",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    actions = {
                                        // Action A: Shared files view trigger
                                        IconButton(onClick = { showSharedLinksScreen = !showSharedLinksScreen }) {
                                            Icon(
                                                imageVector = if (showSharedLinksScreen) Icons.Filled.Share else Icons.Outlined.Share,
                                                contentDescription = "Shared Links",
                                                tint = if (showSharedLinksScreen) CloudPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        // Action B: Recycle bin view trigger
                                        IconButton(onClick = { showRecycleBinScreen = true }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Recycle Bin",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (!showRecycleBinScreen) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == 0 && !showSharedLinksScreen,
                                        onClick = {
                                            activeTab = 0
                                            showSharedLinksScreen = false
                                        },
                                        icon = { Icon(if (activeTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == 1 && !showSharedLinksScreen,
                                        onClick = {
                                            activeTab = 1
                                            filesCategoryJump = "" // Clear filter
                                            showSharedLinksScreen = false
                                        },
                                        icon = { Icon(if (activeTab == 1) Icons.Filled.Folder else Icons.Outlined.Folder, contentDescription = "Files") },
                                        label = { Text("Files", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == 2 && !showSharedLinksScreen,
                                        onClick = {
                                            activeTab = 2
                                            showSharedLinksScreen = false
                                        },
                                        icon = { Icon(if (activeTab == 2) Icons.Filled.SwapVert else Icons.Outlined.SwapVert, contentDescription = "Transfers") },
                                        label = { Text("Transfers", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == 3 && !showSharedLinksScreen,
                                        onClick = {
                                            activeTab = 3
                                            showSharedLinksScreen = false
                                        },
                                        icon = { Icon(if (activeTab == 3) Icons.Filled.Lock else Icons.Outlined.Lock, contentDescription = "Vault") },
                                        label = { Text("Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == 4 && !showSharedLinksScreen,
                                        onClick = {
                                            activeTab = 4
                                            showSharedLinksScreen = false
                                        },
                                        icon = { Icon(if (activeTab == 4) Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser, contentDescription = "Studio") },
                                        label = { Text("Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (showSharedLinksScreen) {
                                // Shared Links Overlay Screen
                                SharedFilesScreen(viewModel = viewModel)
                            } else {
                                when (activeTab) {
                                    0 -> HomeDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToTab = { index ->
                                            activeTab = index
                                        },
                                        onCategoryClick = { cat ->
                                            filesCategoryJump = cat
                                            activeTab = 1 // Go to files
                                        }
                                    )
                                    1 -> FilesManagerScreen(
                                        viewModel = viewModel,
                                        initialCategoryFilter = filesCategoryJump
                                    )
                                    2 -> TransferQueueScreen(viewModel = viewModel)
                                    3 -> VaultScreen(viewModel = viewModel)
                                    4 -> AboutDeveloperScreen()
                                }
                            }

                            // Fullscreen overlay: Recycle Bin
                            AnimatedVisibility(
                                visible = showRecycleBinScreen,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    RecycleBinScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { showRecycleBinScreen = false }
                                    )
                                }
                            }

                            // Fullscreen overlay: Cinema Multimedia Player (for Audio/Video files)
                            MediaPlayerView(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
