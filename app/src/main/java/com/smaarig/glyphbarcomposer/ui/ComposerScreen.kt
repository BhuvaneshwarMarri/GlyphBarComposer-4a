package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerScreen(
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier,
    redViewModel: RedGlyphViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRedOn by redViewModel.isRedOn.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMPOSER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                )
                Text(
                    "Design custom LED sequences",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = viewModel::stopPlayback,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Off",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Painter Card
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GLYPH PAINTER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    IntensityLegend()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.glyphIntensities.forEachIndexed { index, intensity ->
                        val isRedGlyph = index == 6
                        val finalIntensity = if (isRedGlyph) {
                            if (intensity > 0 || isRedOn) 6 else 0
                        } else intensity

                        GlyphBox(
                            label = if (isRedGlyph) "RED" else "A${index + 1}",
                            intensity = finalIntensity,
                            modifier = Modifier.weight(1f),
                            onIntensityChange = { newIntensity ->
                                if (isRedGlyph) {
                                    redViewModel.setRed(newIntensity > 0)
                                    viewModel.onIntensityChange(index, newIntensity)
                                } else {
                                    viewModel.onIntensityChange(index, newIntensity)
                                }
                            },
                            enabled = !uiState.isPlaying,
                            isRed = isRedGlyph
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text("${uiState.durationMs.toInt()}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                    Slider(
                        value = uiState.durationMs,
                        onValueChange = viewModel::onDurationChange,
                        valueRange = 100f..2000f,
                        steps = 18,
                        enabled = !uiState.isPlaying,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF222222)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                PlaybackControls(
                    uiState = uiState,
                    onAddStep = viewModel::addStep,
                    onStartPlayback = { viewModel.startPlayback(uiState.currentSequenceSteps) },
                    onTogglePause = viewModel::togglePause,
                    onStopPlayback = viewModel::stopPlayback
                )
            }
        }

        // Sequence Preview
        if (uiState.currentSequenceSteps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CURRENT TIMELINE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        "Clear",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !uiState.isPlaying) { viewModel.clearSequence() }
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.currentSequenceSteps) { step ->
                        StepPreviewBox(step)
                    }
                }
            }
        }

        // Save Row
        if (uiState.currentSequenceSteps.isNotEmpty()) {
            SaveRow(
                name = uiState.sequenceName,
                onNameChange = viewModel::onSequenceNameChange,
                onSave = viewModel::savePlaylist,
                enabled = !uiState.isPlaying && uiState.sequenceName.isNotBlank()
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun IntensityLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Color(0xFF686868), Color(0xFFCDCDCD), Color(0xFFFFFFFF)).forEach { color ->
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!uiState.isPlaying) {
            Button(
                onClick = onAddStep,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            Button(
                onClick = onStartPlayback,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                enabled = uiState.currentSequenceSteps.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("PREVIEW", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onTogglePause,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isPaused) "RESUME" else "PAUSE", fontWeight = FontWeight.Black)
            }

            Button(
                onClick = onStopPlayback,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(Modifier.width(8.dp))
                Text("STOP", fontWeight = FontWeight.Black)
            }
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
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Sequence Name", color = Color(0xFF444444), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Text("SAVE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun StepPreviewBox(step: GlyphSequence) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${step.durationMs}ms", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { i ->
                    val intensityVal = step.channelIntensities[getChannelForIndex(i)] ?: 0
                    val finalIntensity = if (i == 6 && intensityVal > 0) 6 else intensityVal
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 12.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(intensityColor[finalIntensity])
                    )
                }
            }
        }
    }
}
