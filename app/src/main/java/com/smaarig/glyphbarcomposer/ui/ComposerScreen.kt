package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

// ─── Intensity palette ──────────────────────────────────────────────────────
private val intensityColor = listOf(
    Color(0xFF1C1C1C),   // 0 – OFF
    Color(0xFF686868),   // 1 – LOW
    Color(0xFFCDCDCD),   // 2 – MED
    Color(0xFFFFFFFF),   // 3 – HIGH
)

private val intensityBorder = listOf(
    Color(0xFF3A3A3A),   // 0
    Color(0xFF888888),   // 1
    Color(0xFFE0E0E0),   // 2
    Color(0xFFFFFFFF),   // 3
)

private fun labelColor(intensity: Int) =
    if (intensity >= 2) Color(0xFF111111) else Color(0xFFFFFFFF)

private val statusLabel = listOf("", "LOW", "MED", "HIGH")

@Composable
fun ComposerScreen(
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSequences by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderRow(onStop = viewModel::stopPlayback)

        Spacer(Modifier.height(4.dp))

        IntensityLegend()

        Spacer(Modifier.height(12.dp))

        // ── Step 1: LED Selection ──
        SectionLabel("Step 1 — Select LEDs  (drag ↑↓ to change level, tap to toggle)")

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.glyphIntensities.forEachIndexed { index, intensity ->
                GlyphBox(
                    label = "A${index + 1}",
                    intensity = intensity,
                    modifier = Modifier.weight(1f),
                    onIntensityChange = { newIntensity ->
                        viewModel.onIntensityChange(index, newIntensity)
                    },
                    enabled = !uiState.isPlaying
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Step 2: Duration ──
        SectionLabel("Step 2 — Duration: ${uiState.durationMs.toInt()} ms")
        Slider(
            value = uiState.durationMs,
            onValueChange = viewModel::onDurationChange,
            valueRange = 100f..5000f,
            steps = 48,
            enabled = !uiState.isPlaying,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF3A3A3A)
            )
        )

        // ── Action row ──
        PlaybackControls(
            uiState = uiState,
            onAddStep = viewModel::addStep,
            onStartPlayback = { viewModel.startPlayback(uiState.currentSequenceSteps) },
            onTogglePause = viewModel::togglePause,
            onStopPlayback = viewModel::stopPlayback
        )

        // ── Current sequence steps preview ──
        if (uiState.currentSequenceSteps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            CurrentSequencePreview(
                steps = uiState.currentSequenceSteps,
                isPlaying = uiState.isPlaying,
                onClear = viewModel::clearSequence
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Save ──
        SaveRow(
            name = uiState.sequenceName,
            onNameChange = viewModel::onSequenceNameChange,
            onSave = viewModel::savePlaylist,
            enabled = !uiState.isPlaying && uiState.currentSequenceSteps.isNotEmpty() && uiState.sequenceName.isNotBlank()
        )

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(Modifier.height(10.dp))

        Text(
            "Saved Sequences",
            color = Color(0xFF888888),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))

        SavedSequencesList(
            savedSequences = savedSequences,
            modifier = Modifier.weight(1f),
            onPlay = { steps -> viewModel.startPlayback(steps) },
            onDelete = viewModel::deletePlaylist
        )
    }
}

@Composable
private fun HeaderRow(onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "COMPOSER",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = onStop,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color(0xFFFF5252)
            )
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = "Turn off all lights",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun IntensityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("OFF", "LOW", "MED", "HIGH").forEachIndexed { i, lbl ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(intensityColor[i], RoundedCornerShape(2.dp))
                        .border(1.dp, intensityBorder[i], RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(3.dp))
                Text(lbl, color = Color(0xFF888888), fontSize = 10.sp)
            }
            if (i < 3) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun PlaybackControls(
    uiState: ComposerUiState,
    onAddStep: () -> Unit,
    onStartPlayback: () -> Unit,
    onTogglePause: () -> Unit,
    onStopPlayback: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onAddStep,
            modifier = Modifier.weight(1f),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            enabled = !uiState.isPlaying
        ) {
            Text("+ Step")
        }

        if (!uiState.isPlaying) {
            Button(
                onClick = onStartPlayback,
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.Black
                ),
                enabled = uiState.currentSequenceSteps.isNotEmpty(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Loop")
            }
        } else {
            Button(
                onClick = onTogglePause,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (uiState.isPaused) "Res" else "Pause")
            }

            Button(
                onClick = onStopPlayback,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Stop")
            }
        }
    }
}

@Composable
private fun CurrentSequencePreview(
    steps: List<GlyphSequence>,
    isPlaying: Boolean,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Current Sequence  (${steps.size} steps)",
            color = Color(0xFF888888),
            fontSize = 13.sp
        )
        Text(
            "Clear",
            color = if (isPlaying) Color(0xFF444444) else Color(0xFFFF5252),
            fontSize = 12.sp,
            modifier = Modifier.clickable(enabled = !isPlaying) { onClear() }
        )
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(steps) { step ->
            StepPreviewBox(step)
        }
    }
}

@Composable
private fun SaveRow(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Sequence name", color = Color(0xFF888888), fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color(0xFF444444)
            )
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            enabled = enabled
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun SavedSequencesList(
    savedSequences: List<PlaylistWithSteps>,
    modifier: Modifier = Modifier,
    onPlay: (List<GlyphSequence>) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(savedSequences) { playlistWithSteps ->
            SavedSequenceRow(
                playlistWithSteps = playlistWithSteps,
                onPlay = {
                    onPlay(playlistWithSteps.steps.sortedBy { it.stepIndex }.map { step ->
                        GlyphSequence(step.channelIntensities, step.durationMs)
                    })
                },
                onDelete = { onDelete(playlistWithSteps.playlist) }
            )
        }
    }
}

@Composable
fun GlyphBox(
    label: String,
    intensity: Int,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (intensity > 0) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val fillColor by animateColorAsState(
        targetValue = intensityColor[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "fill"
    )
    val borderColor by animateColorAsState(
        targetValue = intensityBorder[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "border"
    )

    val dragProgress = (kotlin.math.abs(accumulatedDrag).coerceAtMost(30f) / 30f)
    val dragOverlayAlpha = dragProgress * 0.18f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(fillColor)
            .then(if (enabled) {
                Modifier
                    .pointerInput(intensity) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDrag += dragAmount
                                val threshold = 28f
                                if (accumulatedDrag > threshold) {
                                    if (intensity > 0) onIntensityChange(intensity - 1)
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag < -threshold) {
                                    if (intensity < 3) onIntensityChange(intensity + 1)
                                    accumulatedDrag = 0f
                                }
                            },
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f }
                        )
                    }
                    .clickable {
                        if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
                    }
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dragOverlayAlpha > 0f && enabled) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (accumulatedDrag < 0) Color.White.copy(alpha = dragOverlayAlpha)
                        else Color.Black.copy(alpha = dragOverlayAlpha)
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor(intensity)
            )
            if (intensity > 0) {
                Text(
                    text = statusLabel[intensity],
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor(intensity).copy(alpha = 0.75f),
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun StepPreviewBox(step: GlyphSequence) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${step.durationMs}ms", fontSize = 8.sp, color = Color(0xFF777777))
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(6) { i ->
                    val intensity = step.channelIntensities[getChannelForIndex(i)] ?: 0
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(intensityColor[intensity])
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSequenceRow(
    playlistWithSteps: PlaylistWithSteps,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        playlistWithSteps.playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${playlistWithSteps.steps.size} steps",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Mini step strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(playlistWithSteps.steps.sortedBy { it.stepIndex }) { step ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                            .background(Color(0xFF222222)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(6) { i ->
                                val intensity = step.channelIntensities[getChannelForIndex(i)] ?: 0
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = 8.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(intensityColor[intensity])
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
}

fun getChannelForIndex(index: Int): Int = when (index) {
    0 -> com.nothing.ketchum.Glyph.Code_25111.A_1
    1 -> com.nothing.ketchum.Glyph.Code_25111.A_2
    2 -> com.nothing.ketchum.Glyph.Code_25111.A_3
    3 -> com.nothing.ketchum.Glyph.Code_25111.A_4
    4 -> com.nothing.ketchum.Glyph.Code_25111.A_5
    5 -> com.nothing.ketchum.Glyph.Code_25111.A_6
    else -> 0
}
