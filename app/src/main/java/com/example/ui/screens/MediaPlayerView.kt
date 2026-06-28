package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntity
import com.example.ui.theme.CloudCyan
import com.example.ui.theme.CloudPrimary
import com.example.ui.viewmodel.CloudfinityViewModel
import java.util.Locale

@Composable
fun MediaPlayerView(viewModel: CloudfinityViewModel) {
    val activeFile by viewModel.activePlayerFile.collectAsState()
    val isPlaying by viewModel.playerPlaying.collectAsState()
    val progress by viewModel.playerProgress.collectAsState()
    val currentTime by viewModel.playerCurrentTime.collectAsState()

    if (activeFile == null) return

    val file = activeFile!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B13).copy(alpha = 0.98f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Close Player Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { viewModel.closeMediaPlayer() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Player Canvas / Video Simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 10f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF131B2D), Color(0xFF0F1420))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // If video, show screen with glowing cloud. If audio, show soundwaves!
                if (file.isVideo) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = CloudCyan.copy(alpha = 0.8f),
                                modifier = Modifier.size(54.dp)
                            )
                            if (isPlaying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    color = CloudCyan,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Play Simulation Active • mp4 player",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CloudCyan
                        )
                    }
                } else {
                    // Audio Waveform mock
                    Row(
                        modifier = Modifier.padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..18) {
                            val waveHeight = if (isPlaying) (12..64).random().dp else 12.dp
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(waveHeight)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(CloudPrimary, CloudCyan)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Metadata
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (file.isVideo) "Video File • Ultra HD" else "Audio Soundtrack • Hi-Fi Stereo",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Timeline Seeker Slider
            Slider(
                value = progress,
                onValueChange = { viewModel.seekPlayer(it) },
                colors = SliderDefaults.colors(
                    thumbColor = CloudCyan,
                    activeTrackColor = CloudCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timeline values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatPlaybackTime(currentTime), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Text(text = formatPlaybackTime(file.videoDuration), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Media control triggers (Rewind, Play, Skip, Loop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newProgress = (progress - 0.1f).coerceAtLeast(0.0f)
                        viewModel.seekPlayer(newProgress)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                FloatingActionButton(
                    onClick = { viewModel.togglePlayerPlayPause() },
                    containerColor = CloudCyan,
                    contentColor = Color(0xFF070B13),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        val newProgress = (progress + 0.1f).coerceAtMost(1.0f)
                        viewModel.seekPlayer(newProgress)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.Forward10, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
