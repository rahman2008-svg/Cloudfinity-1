package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntity
import com.example.ui.theme.CloudCyan
import com.example.ui.theme.CloudPrimary
import com.example.ui.viewmodel.CloudfinityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: CloudfinityViewModel,
    onNavigateToTab: (Int) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val currentFiles by viewModel.currentFiles.collectAsState()
    val allTransfers by viewModel.allTransfers.collectAsState()
    
    // Calculate size of uploaded files
    val totalUploadedBytes = remember(currentFiles) {
        currentFiles.filter { !it.isFolder }.sumOf { it.size }
    }

    // Simulated base cloud usage of 120.45 GB
    val baseSimulatedBytes = 129_334_660_000L // ~120.45 GB
    val totalUsedBytes = baseSimulatedBytes + totalUploadedBytes
    val maxCloudStorageBytes = 1_099_511_627_776L // 1 TB in bytes

    val storagePercentage = (totalUsedBytes.toFloat() / maxCloudStorageBytes.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = storagePercentage,
        animationSpec = tween(durationMillis = 1000)
    )

    // Size category breakdown
    val photoBytes = baseSimulatedBytes / 4 + currentFiles.filter { it.isImage }.sumOf { it.size }
    val videoBytes = baseSimulatedBytes / 2 + currentFiles.filter { it.isVideo }.sumOf { it.size }
    val audioBytes = baseSimulatedBytes / 8 + currentFiles.filter { it.isAudio }.sumOf { it.size }
    val docBytes = baseSimulatedBytes / 10 + currentFiles.filter { it.isDocument }.sumOf { it.size }

    val recentFiles = remember(currentFiles) {
        currentFiles.filter { !it.isFolder }.sortedByDescending { it.createdTime }.take(4)
    }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cloud Storage Circle Monitor Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Cloud Space Monitor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(90.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = CloudPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f%%", storagePercentage * 100),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CloudPrimary
                                )
                                Text(
                                    text = "Used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Space readouts
                        Column {
                            Text(
                                text = "1 TB Cloud Storage Free",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CloudPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatSizeGB(totalUsedBytes),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of 1,024.00 GB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Storage Breakdown",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multi-segmented bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val totalBytesSum = photoBytes + videoBytes + audioBytes + docBytes
                        val photoWeight = (photoBytes.toFloat() / totalBytesSum.toFloat()).coerceIn(0.1f, 0.5f)
                        val videoWeight = (videoBytes.toFloat() / totalBytesSum.toFloat()).coerceIn(0.1f, 0.5f)
                        val audioWeight = (audioBytes.toFloat() / totalBytesSum.toFloat()).coerceIn(0.1f, 0.5f)
                        val docWeight = (docBytes.toFloat() / totalBytesSum.toFloat()).coerceIn(0.1f, 0.5f)

                        Box(modifier = Modifier.weight(photoWeight).fillMaxHeight().background(Color(0xFF2563EB)))
                        Box(modifier = Modifier.weight(videoWeight).fillMaxHeight().background(Color(0xFFEA580C)))
                        Box(modifier = Modifier.weight(audioWeight).fillMaxHeight().background(Color(0xFF7C3AED)))
                        Box(modifier = Modifier.weight(docWeight).fillMaxHeight().background(Color(0xFF059669)))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breakdown legends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(name = "Photos", size = formatSizeGB(photoBytes), color = Color(0xFF2563EB))
                        LegendItem(name = "Videos", size = formatSizeGB(videoBytes), color = Color(0xFFEA580C))
                        LegendItem(name = "Music", size = formatSizeGB(audioBytes), color = Color(0xFF7C3AED))
                        LegendItem(name = "Docs", size = formatSizeGB(docBytes), color = Color(0xFF059669))
                    }
                }
            }
        }

        // 2. Beautiful Promoted Developer Banner (Prince AR Abdur Rahman)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(4) }, // Go to About Dev Tab
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CloudPrimary.copy(alpha = 0.95f), CloudCyan.copy(alpha = 0.9f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Verified Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Prince AR Abdur Rahman",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Independent Android Developer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "NexVora Lab's primary platform creator. Passionate about building fast, privacy-focused, and beautifully custom productivity tools.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { openWhatsApp(context, "01707424006") },
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { openUrl(context, "https://www.facebook.com/share/1BNn32qoJo/") },
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Facebook", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "View Studio >",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- MOBILE LIVE DEVICE LINK MONITOR CARD ---
        item {
            val systemContext = LocalContext.current
            
            // 1. Real System Storage Metrics
            val localStorageStat = remember {
                try {
                    val path = android.os.Environment.getDataDirectory()
                    val stat = android.os.StatFs(path.path)
                    val blockSize = stat.blockSizeLong
                    val totalBlocks = stat.blockCountLong
                    val availableBlocks = stat.availableBlocksLong
                    val totalBytes = totalBlocks * blockSize
                    val freeBytes = availableBlocks * blockSize
                    val usedBytes = totalBytes - freeBytes
                    Triple(usedBytes, totalBytes, freeBytes)
                } catch (e: Exception) {
                    Triple(48_300_000_000L, 128_000_000_000L, 79_700_000_000L) // fallback
                }
            }
            
            // 2. Real System Battery Metrics
            val systemBatteryStat = remember(systemContext) {
                try {
                    val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    val batteryStatus = systemContext.registerReceiver(null, filter)
                    val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 75
                    val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    percent to isCharging
                } catch (e: Exception) {
                    75 to false
                }
            }
            
            // 3. Real System Network Metrics
            val systemNetworkStat = remember(systemContext) {
                try {
                    val connMgr = systemContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val activeNet = connMgr.activeNetworkInfo
                    if (activeNet != null && activeNet.isConnected) {
                        if (activeNet.type == android.net.ConnectivityManager.TYPE_WIFI) {
                            "Wi-Fi Connection"
                        } else if (activeNet.type == android.net.ConnectivityManager.TYPE_MOBILE) {
                            "Cellular Network"
                        } else {
                            "Ethernet Port"
                        }
                    } else {
                        "Offline Storage"
                    }
                } catch (e: Exception) {
                    "Wi-Fi Connection"
                }
            }
            
            // 4. Live Storage Permission Check
            var mediaPermissionGranted by remember {
                mutableStateOf(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            systemContext,
                            android.Manifest.permission.READ_MEDIA_IMAGES
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            systemContext,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                )
            }

            val mediaPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { resultsMap ->
                mediaPermissionGranted = resultsMap.values.all { it }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mobile Live Device Link",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Link Status Badge
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF10B981),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Linked", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This app observes and connects directly with your real mobile device system, network, battery status, and local media files dynamically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card A: Storage
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = CloudPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Real Storage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "%.1f GB Free", localStorageStat.third.toFloat() / (1024f * 1024f * 1024f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Card B: Battery
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(
                                    imageVector = if (systemBatteryStat.second) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = if (systemBatteryStat.second) Color(0xFF10B981) else CloudPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Battery Power", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${systemBatteryStat.first}% ${if (systemBatteryStat.second) "⚡" else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Card C: Network Link
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(
                                    imageVector = if (systemNetworkStat.contains("Wi-Fi")) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = CloudPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Network", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = systemNetworkStat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Permission status & grant button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Files Permission Link",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (mediaPermissionGranted) "Full Connection Activated" else "Access Restricted",
                                fontSize = 10.sp,
                                color = if (mediaPermissionGranted) Color(0xFF10B981) else Color(0xFFEA580C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (!mediaPermissionGranted) {
                            Button(
                                onClick = {
                                    val reqs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VIDEO,
                                            android.Manifest.permission.READ_MEDIA_AUDIO
                                        )
                                    } else {
                                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    mediaPermissionLauncher.launch(reqs)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CloudPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Connect Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Connected to Files", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Category Shortcuts Row
        item {
            Column {
                Text(
                    text = "Quick Categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CategoryPill(
                        name = "Photos",
                        icon = Icons.Outlined.Image,
                        color = Color(0xFF2563EB),
                        onClick = { onCategoryClick("photo") }
                    )
                    CategoryPill(
                        name = "Videos",
                        icon = Icons.Outlined.VideoFile,
                        color = Color(0xFFEA580C),
                        onClick = { onCategoryClick("video") }
                    )
                    CategoryPill(
                        name = "Music",
                        icon = Icons.Outlined.Audiotrack,
                        color = Color(0xFF7C3AED),
                        onClick = { onCategoryClick("music") }
                    )
                    CategoryPill(
                        name = "Documents",
                        icon = Icons.Outlined.Description,
                        color = Color(0xFF059669),
                        onClick = { onCategoryClick("document") }
                    )
                    CategoryPill(
                        name = "Safe Vault",
                        icon = Icons.Outlined.Lock,
                        color = Color(0xFFE11D48),
                        onClick = { onNavigateToTab(3) } // Vault Tab
                    )
                }
            }
        }

        // 4. Recent Uploads Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("View All Files", color = CloudPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No files uploaded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(recentFiles) { file ->
                RecentFileRow(file = file, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CategoryPill(
    name: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LegendItem(name: String, size: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(text = name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(text = size, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun RecentFileRow(file: FileEntity, viewModel: CloudfinityViewModel) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(file.createdTime))

    val icon = when {
        file.isImage -> Icons.Default.Image
        file.isVideo -> Icons.Default.VideoFile
        file.isAudio -> Icons.Default.Audiotrack
        file.isDocument -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }

    val iconColor = when {
        file.isImage -> Color(0xFF2563EB)
        file.isVideo -> Color(0xFFEA580C)
        file.isAudio -> Color(0xFF7C3AED)
        file.isDocument -> Color(0xFF059669)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .clickable {
                if (file.isVideo || file.isAudio) {
                    viewModel.openMediaPlayer(file)
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatSize(file.size)} • $formattedDate",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        if (file.isVideo || file.isAudio) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play",
                tint = CloudPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val groupIndex = digitGroups.coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, groupIndex.toDouble()), units[groupIndex])
}

fun formatSizeGB(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.2f GB", gb)
}
