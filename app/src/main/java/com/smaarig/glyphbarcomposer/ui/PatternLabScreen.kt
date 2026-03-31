package com.smaarig.glyphbarcomposer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp)) {
        Text("Pattern Lab", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Scrollable content area
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
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
            }

            item {
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
            }

            // Merge Mode
            item {
                Column {
                    Text("MERGE MODE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeButton("Sequential", !uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(false) }
                        ModeButton("Layered", uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(true) }
                    }
                }
            }
        }

        // Preview & Save
        if (uiState.previewSteps.isNotEmpty()) {
            SaveResultSection(
                name = uiState.resultName,
                onNameChange = viewModel::onResultNameChange,
                onSave = viewModel::saveResult,
                isPlaying = uiState.isPlaying,
                onTogglePreview = viewModel::togglePreview
            )
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
        color = Color(0xFF161616),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { onPick() }) {
                    Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(name ?: "Select Sequence...", color = if (name != null) Color.White else Color(0xFF555555), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                }
                
                // Toggle row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onInvertChange(!inverted) }) {
                        Icon(
                            if (inverted) Icons.Default.InvertColors else Icons.Default.InvertColorsOff,
                            contentDescription = "Invert",
                            tint = if (inverted) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { onMirrorChange(!mirrored) }) {
                        Icon(
                            Icons.Default.Flip,
                            contentDescription = "Mirror",
                            tint = if (mirrored) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Speed Slider
            LabSliderRow(
                label = "Speed",
                valueText = "${"%.1f".format(speed)}x",
                value = speed,
                range = 0.5f..2.0f,
                onValueChange = onSpeedChange
            )

            Spacer(Modifier.height(8.dp))

            // Brightness Slider
            LabSliderRow(
                label = "Light",
                valueText = "${(brightness * 100).toInt()}%",
                value = brightness,
                range = 0.0f..1.5f,
                onValueChange = onBrightnessChange
            )

            Spacer(Modifier.height(8.dp))

            // Repeats Slider
            LabSliderRow(
                label = "Repeat",
                valueText = "${repeats}x",
                value = repeats.toFloat(),
                range = 1f..5f,
                steps = 3,
                onValueChange = { onRepeatsChange(it.toInt()) }
            )
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(valueText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                inactiveTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(44.dp).clickable { onClick() },
        color = if (selected) Color.White else Color(0xFF161616),
        shape = RoundedCornerShape(8.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF2A2A2A)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Button(
            onClick = onTogglePreview,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFFFF5252) else Color(0xFF00C853))
        ) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isPlaying) "Stop Preview" else "Preview Lab Result", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Result name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF444444)
                )
            )
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier.size(52.dp).clickable(enabled = name.isNotBlank()) { onSave() },
                color = if (name.isNotBlank()) Color.White else Color(0xFF161616),
                shape = RoundedCornerShape(12.dp),
                contentColor = if (name.isNotBlank()) Color.Black else Color.Gray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
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
        title = { Text("Select Sequence", color = Color.White) },
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
                            Text(p.playlist.name, modifier = Modifier.padding(16.dp), color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = Color(0xFF161616)
    )
}
