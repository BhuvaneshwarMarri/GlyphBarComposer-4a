package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 16.dp else 24.dp)
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
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Sidebar: Controls
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SidebarControls(
                    uiState = uiState,
                    onAddStep = viewModel::addStep,
                    onStartPlayback = { viewModel.startPlayback(uiState.currentSequenceSteps) },
                    onTogglePause = viewModel::togglePause,
                    onStopPlayback = viewModel::stopPlayback,
                    onClearTimeline = viewModel::clearSequence
                )
                
                Spacer(Modifier.weight(1f))

                if (uiState.currentSequenceSteps.isNotEmpty()) {
                    SaveSection(
                        name = uiState.sequenceName,
                        onNameChange = viewModel::onSequenceNameChange,
                        onSave = viewModel::savePlaylist,
                        enabled = !uiState.isPlaying && uiState.sequenceName.isNotBlank()
                    )
                }
            }

            // Right Area: Painter & Timeline
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PainterCard(uiState, isRedOn, viewModel, redViewModel)
                
                if (uiState.currentSequenceSteps.isNotEmpty()) {
                    TimelinePreviewSection(uiState, viewModel)
                } else {
                    EmptyTimelinePlaceholder()
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SidebarControls(
    uiState: ComposerUiState,
    onAddStep: () -> Unit,
    onStartPlayback: () -> Unit,
    onTogglePause: () -> Unit,
    onStopPlayback: () -> Unit,
    onClearTimeline: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.isPlaying) {
            SidebarButton(Icons.Default.Add, "ADD STEP", Color.White, Color(0xFF1A1A1A), onAddStep)
            SidebarButton(Icons.Default.PlayArrow, "PREVIEW", Color.Black, Color.White, onStartPlayback, enabled = uiState.currentSequenceSteps.isNotEmpty())
            SidebarButton(Icons.Default.DeleteSweep, "CLEAR", Color(0xFFFF5252), Color(0x1AFF5252), onClearTimeline, enabled = uiState.currentSequenceSteps.isNotEmpty())
        } else {
            SidebarButton(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, if (uiState.isPaused) "RESUME" else "PAUSE", Color.Black, Color.White, onTogglePause)
            SidebarButton(Icons.Default.Stop, "STOP", Color(0xFFFF5252), Color(0x1AFF5252), onStopPlayback)
        }
    }
}

@Composable
private fun SidebarButton(
    icon: ImageVector,
    label: String,
    contentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(0.3f),
            disabledContentColor = contentColor.copy(0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Black, fontSize = 10.sp)
    }
}

@Composable
private fun SaveSection(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SAVE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        TextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Name", color = Color(0xFF444444), fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled
        ) {
            Text("SAVE MIX", fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PainterCard(
    uiState: ComposerUiState,
    isRedOn: Boolean,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
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

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (isRedGlyph) "RED" else "A${index + 1}", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        IntensityWheelPicker(
                            intensity = if (isRedGlyph && finalIntensity > 0) 3 else intensity,
                            onIntensityChange = { newIntensity ->
                                if (isRedGlyph) {
                                    redViewModel.setRed(newIntensity > 0)
                                    viewModel.onIntensityChange(index, newIntensity)
                                } else {
                                    viewModel.onIntensityChange(index, newIntensity)
                                }
                            },
                            enabled = !uiState.isPlaying,
                            isRed = isRedGlyph,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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

            // Controls integrated into PainterCard are removed since they are now in Sidebar
        }
    }
}

@Composable
private fun TimelinePreviewSection(uiState: ComposerUiState, viewModel: ComposerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TIMELINE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(uiState.currentSequenceSteps.size) { index ->
                val step = uiState.currentSequenceSteps[index]
                StepPreviewBox(
                    step = step,
                    onDelete = { viewModel.removeStep(index) },
                    onLoad = { viewModel.loadStep(index) },
                    onInsertAfter = { viewModel.insertStepAt(index + 1) },
                    enabled = !uiState.isPlaying
                )
            }
        }
    }
}

@Composable
private fun EmptyTimelinePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF111111))
            .border(BorderStroke(1.dp, Color(0xFF1A1A1A)), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Layers, null, tint = Color(0xFF222222), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Timeline is empty", color = Color(0xFF333333), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Add steps to build sequence", color = Color(0xFF222222), fontSize = 11.sp)
        }
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
fun StepPreviewBox(
    step: GlyphSequence,
    onDelete: () -> Unit,
    onLoad: () -> Unit,
    onInsertAfter: () -> Unit,
    enabled: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { showMenu = true },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${step.durationMs}ms", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { i ->
                    val intensityVal = step.channelIntensities[getChannelForIndex(i)] ?: 0
                    val finalIntensity = if (i == 6 && intensityVal > 0) 6 else intensityVal
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 16.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(intensityColor[finalIntensity])
                    )
                }
            }
        }

        if (showMenu) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable { showMenu = false },
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onLoad(); showMenu = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { onInsertAfter(); showMenu = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { onDelete(); showMenu = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
