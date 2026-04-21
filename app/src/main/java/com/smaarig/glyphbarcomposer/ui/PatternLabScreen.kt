package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel

@Composable
fun PatternLabScreen(viewModel: PatternLabViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    var showDialogA by remember { mutableStateOf(false) }
    var showDialogB by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PATTERN LAB",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                    )
                    Text(
                        "Mix and remix sequences",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                
                if (uiState.isPlaying) {
                    IconButton(
                        onClick = viewModel::togglePreview,
                        modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252))
                    }
                }
            }

            // Scrollable area
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BaseControlCard(
                    title = "BASE A",
                    name = uiState.selectedPlaylistA?.playlist?.name,
                    speed = uiState.speedMultiplierA,
                    brightness = uiState.brightnessA,
                    repeats = uiState.repeatsA,
                    inverted = uiState.isInvertedA,
                    mirrored = uiState.isMirroredA,
                    onPick = { showDialogA = true },
                    onSpeedChange = viewModel::onSpeedMultiplierAChange,
                    onBrightnessChange = viewModel::onBrightnessAChange,
                    onRepeatsChange = viewModel::onRepeatsAChange,
                    onInvertChange = viewModel::onInvertAChange,
                    onMirrorChange = viewModel::onMirrorAChange
                )

                BaseControlCard(
                    title = "BASE B",
                    name = uiState.selectedPlaylistB?.playlist?.name,
                    speed = uiState.speedMultiplierB,
                    brightness = uiState.brightnessB,
                    repeats = uiState.repeatsB,
                    inverted = uiState.isInvertedB,
                    mirrored = uiState.isMirroredB,
                    onPick = { showDialogB = true },
                    onSpeedChange = viewModel::onSpeedMultiplierBChange,
                    onBrightnessChange = viewModel::onBrightnessBChange,
                    onRepeatsChange = viewModel::onRepeatsBChange,
                    onInvertChange = viewModel::onInvertBChange,
                    onMirrorChange = viewModel::onMirrorBChange
                )

                // Merge Mode
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MERGE MODE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModeButton("Sequential", !uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(false) }
                        ModeButton("Layered", uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(true) }
                    }
                }

                // Preview & Save Result
                if (uiState.previewSteps.isNotEmpty()) {
                    SaveResultSection(
                        name = uiState.resultName,
                        onNameChange = viewModel::onResultNameChange,
                        onSave = viewModel::saveResult,
                        isPlaying = uiState.isPlaying,
                        onTogglePreview = viewModel::togglePreview
                    )
                }
                
                Spacer(Modifier.height(120.dp))
            }
        }
    }

    if (showDialogA) {
        PlaylistPickerDialog(
            playlists = allPlaylists,
            onSelect = { 
                viewModel.selectPlaylistA(it)
                showDialogA = false 
            },
            onDismiss = { showDialogA = false }
        )
    }
    if (showDialogB) {
        PlaylistPickerDialog(
            playlists = allPlaylists,
            onSelect = { 
                viewModel.selectPlaylistB(it)
                showDialogB = false 
            },
            onDismiss = { showDialogB = false }
        )
    }
}

@Composable
private fun BaseControlCard(
    title: String,
    name: String?,
    speed: Float,
    brightness: Float,
    repeats: Int,
    inverted: Boolean,
    mirrored: Boolean,
    onPick: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onRepeatsChange: (Int) -> Unit,
    onInvertChange: (Boolean) -> Unit,
    onMirrorChange: (Boolean) -> Unit
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { onPick() }) {
                    Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(name ?: "Select Sequence...", color = if (name != null) Color.White else Color(0xFF444444), fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onInvertChange(!inverted) },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (inverted) Color.White else Color(0xFF1A1A1A))
                    ) {
                        Icon(
                            if (inverted) Icons.Default.InvertColors else Icons.Default.InvertColorsOff,
                            null,
                            tint = if (inverted) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onMirrorChange(!mirrored) },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (mirrored) Color.White else Color(0xFF1A1A1A))
                    ) {
                        Icon(
                            Icons.Default.Flip,
                            null,
                            tint = if (mirrored) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabSliderRow("Speed", "${"%.1f".format(speed)}x", speed, 0.5f..2.0f, onValueChange = onSpeedChange)
                LabSliderRow("Light", "${(brightness * 100).toInt()}%", brightness, 0.0f..1.5f, onValueChange = onBrightnessChange)
                LabSliderRow("Repeat", "${repeats}x", repeats.toFloat(), 1f..5f, steps = 3, onValueChange = { onRepeatsChange(it.toInt()) })
            }
        }
    }
}

@Composable
private fun LabSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 15,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.width(50.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(valueText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF222222)
            )
        )
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(48.dp).clickable { onClick() },
        color = if (selected) Color.White else Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF222222)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SaveResultSection(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    isPlaying: Boolean,
    onTogglePreview: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onTogglePreview,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFFFF5252) else Color.White, contentColor = if (isPlaying) Color.White else Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isPlaying) "STOP PREVIEW" else "PREVIEW MIX", fontWeight = FontWeight.Black)
        }

        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Result Name", color = Color(0xFF444444), fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
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
                    enabled = name.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text("SAVE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistWithSteps>,
    onSelect: (PlaylistWithSteps) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Sequence", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            if (playlists.isEmpty()) {
                Text("No saved sequences found.", color = Color.Gray)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(playlists) { p ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(p) },
                            color = Color.Transparent
                        ) {
                            Text(p.playlist.name, modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White, fontWeight = FontWeight.Black) }
        },
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp)
    )
}
