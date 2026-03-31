package com.smaarig.glyphbarcomposer.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.smaarig.glyphbarcomposer.data.MusicSyncEvent
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicSyncUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicSyncViewModel

// ─── Intensity palette (shared with ComposerScreen) ─────────────────────────
private val intensityColor = listOf(
    Color(0xFF1C1C1C),
    Color(0xFF686868),
    Color(0xFFCDCDCD),
    Color(0xFFFFFFFF),
)

@Composable
fun MusicSyncScreen(
    viewModel: MusicSyncViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visualizerData by viewModel.visualizerData.collectAsStateWithLifecycle()
    val audioPositionMs by viewModel.audioPositionMs.collectAsStateWithLifecycle()
    val glyphIntensities by viewModel.glyphIntensities.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) viewModel.retryVisualizerSetup()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.isAudioPlaying) {
            viewModel.retryVisualizerSetup()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "Unknown Song"
            viewModel.loadSong(it, name)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.audioUri == null) {
            // ── Empty state ──
            EmptyMusicState(onPickFile = { launcher.launch("audio/*") })
        } else {
            // ── Main content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Music Sync",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    // Save project button
                    if (uiState.musicEvents.isNotEmpty()) {
                        TextButton(
                            onClick = viewModel::saveMusicProject,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00C853))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Player card ──
                MusicPlayerCard(
                    uiState = uiState,
                    audioPositionMs = audioPositionMs,
                    onPlayPause = viewModel::toggleMusicPlayback,
                    onSeek = viewModel::seekMusic,
                    onChangeAudio = { launcher.launch("audio/*") }
                )

                Spacer(Modifier.height(12.dp))

                // Visualizer
                FrequencyVisualizer(visualizerData)

                Spacer(Modifier.height(14.dp))

                ManualModeContent(
                    uiState = uiState,
                    audioPositionMs = audioPositionMs,
                    glyphIntensities = glyphIntensities,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Analysis overlay ──
        AnimatedVisibility(
            visible = uiState.isAnalyzing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.height(20.dp))
                    Text("Generating Glyph Sequence…", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Analyzing beats for the entire track", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        // ── Ready badge ──
        val showReadyMessage = uiState.isAnalysisComplete && !uiState.isPlaybackStarted && !uiState.isAnalyzing
        AnimatedVisibility(
            visible = showReadyMessage,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                color = Color(0xFF00C853),
                contentColor = Color.Black,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ready — press play to sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MusicPlayerCard(
    uiState: MusicSyncUiState,
    audioPositionMs: Int,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onChangeAudio: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF161616),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Album art placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF252525)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        uiState.audioName ?: "Unknown",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatTime(audioPositionMs)} / ${formatTime(uiState.audioDurationMs)}",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }

                // Change audio button — explicitly labelled
                OutlinedButton(
                    onClick = onChangeAudio,
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Change", fontSize = 12.sp)
                }

                Spacer(Modifier.width(4.dp))

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isAnalysisComplete) Color.White else Color(0xFF333333))
                        .clickable(enabled = uiState.isAnalysisComplete) { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (uiState.isAnalysisComplete) Color.Black else Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Seek bar
            Slider(
                value = audioPositionMs.toFloat(),
                onValueChange = onSeek,
                valueRange = 0f..uiState.audioDurationMs.toFloat().coerceAtLeast(1f),
                enabled = uiState.isAnalysisComplete,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Visualizer
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FrequencyVisualizer(magnitudes: List<Float>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0E0E0E)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(Modifier.width(4.dp))
        magnitudes.forEach { magnitude ->
            val animatedHeight by animateFloatAsState(
                targetValue = (magnitude / 100f).coerceIn(0.04f, 1f),
                animationSpec = tween(durationMillis = 60, easing = LinearEasing),
                label = "barHeight"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (magnitude > 65)
                                listOf(Color(0xFF00C853), Color(0xFF69F0AE))
                            else
                                listOf(Color(0xFF444444), Color(0xFF666666))
                        ),
                        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Manual mode
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ManualModeContent(
    uiState: MusicSyncUiState,
    audioPositionMs: Int,
    glyphIntensities: List<Int>,
    viewModel: MusicSyncViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll events list to bottom when new event added
    LaunchedEffect(uiState.musicEvents.size) {
        if (uiState.musicEvents.isNotEmpty()) {
            listState.animateScrollToItem(uiState.musicEvents.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── LED pad ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LED Pattern",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Drag ↑↓ to set intensity, tap to toggle",
                color = Color(0xFF555555),
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            glyphIntensities.forEachIndexed { index, intensity ->
                GlyphBox(
                    label = "A${index + 1}",
                    intensity = intensity,
                    modifier = Modifier.weight(1f),
                    onIntensityChange = { viewModel.onIntensityChange(index, it) },
                    enabled = !uiState.isAudioPlaying
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Mark beat button ──
        Button(
            onClick = viewModel::addMusicEvent,
            modifier = Modifier.fillMaxWidth(),
            enabled = glyphIntensities.any { it > 0 } && uiState.isAnalysisComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00C853),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF1A2E1A),
                disabledContentColor = Color(0xFF3A5A3A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (uiState.isAnalysisComplete)
                    "Add Beat at ${formatTime(audioPositionMs)}"
                else
                    "Generating...",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Events timeline ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Timeline  (${uiState.musicEvents.size} events)",
                color = Color(0xFF888888),
                fontSize = 11.sp
            )
            if (uiState.musicEvents.isNotEmpty()) {
                Text(
                    "Clear all",
                    color = Color(0xFF663333),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { viewModel.clearAllMusicEvents() }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFF252525), RoundedCornerShape(14.dp))
        ) {
            if (uiState.musicEvents.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF2A2A2A),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No events yet", color = Color(0xFF3A3A3A), fontSize = 13.sp)
                    Text(
                        "Play the song and mark beats above",
                        color = Color(0xFF2A2A2A),
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.musicEvents, key = { it.timestampMs }) { event ->
                        val isActive = uiState.isAudioPlaying &&
                                audioPositionMs.toLong() >= event.timestampMs &&
                                audioPositionMs.toLong() < event.timestampMs + 800
                        TimelineEventRow(
                            event = event,
                            isActive = isActive,
                            onDelete = { viewModel.deleteMusicEvent(event) }
                        )
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Timeline row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimelineEventRow(
    event: MusicSyncEvent,
    isActive: Boolean,
    onDelete: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF003320) else Color(0xFF1A1A1A),
        animationSpec = tween(150),
        label = "rowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF00C853) else Color.Transparent,
        animationSpec = tween(150),
        label = "rowBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            formatTime(event.timestampMs.toInt()),
            color = if (isActive) Color(0xFF00C853) else Color(0xFF888888),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(40.dp)
        )

        Spacer(Modifier.width(12.dp))

        // LED intensity mini bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { i ->
                val ch = getChannelForIndex(i)
                val intensity = event.channelIntensities[ch] ?: 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height((4 + intensity * 4).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(intensityColor[intensity])
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Duration badge
        Text(
            "${event.durationMs}ms",
            color = Color(0xFF555555),
            fontSize = 10.sp
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFF662222),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyMusicState(onPickFile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Music Sync",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Sync Glyph lights to any audio file.",
                color = Color(0xFF888888),
                fontSize = 14.sp
            )
            Text(
                "Mark beats manually or let auto mode react to bass.",
                color = Color(0xFF555555),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp).padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Choose Audio File", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Supports MP3, AAC, FLAC, WAV",
                color = Color(0xFF3A3A3A),
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTime(ms: Int): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}

// Re-export so LibraryScreen can reference animateColorAsState without import conflicts
@Composable
private fun animateColorAsState(
    targetValue: Color,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Color>,
    label: String
) = androidx.compose.animation.animateColorAsState(targetValue, animationSpec, label = label)
