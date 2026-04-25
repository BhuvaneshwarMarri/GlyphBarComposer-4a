package com.smaarig.glyphbarcomposer.ui.composer.components.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.composer.components.v2.DraggableTimeline
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerScreenOldLandscape(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Sequence", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("Sequence Name", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (fileName.isNotBlank()) {
                        viewModel.savePlaylist(fileName)
                        showSaveDialog = false
                        fileName = ""
                    }
                }) {
                    Text("SAVE", color = Color(0xFF00C853), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp)
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Controls (Scrollable to prevent overflow)
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0C0C0C))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("GLYPHS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(6) { index ->
                        OldGlyphButton(
                            index = index,
                            intensity = uiState.glyphIntensities[index],
                            isSelected = uiState.selectedChannelIndex == index,
                            isRed = false,
                            onIntensityChange = { newVal ->
                                viewModel.onIntensityChange(index, newVal)
                                viewModel.setSelectedChannel(index)
                            },
                            onSelect = { viewModel.setSelectedChannel(index) },
                            enabled = !uiState.isPlaying
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(52.dp)
                            .background(Color(0xFF222222))
                    )

                    OldGlyphButton(
                        index = 6,
                        intensity = uiState.glyphIntensities[6],
                        isSelected = uiState.selectedChannelIndex == 6,
                        isRed = true,
                        onIntensityChange = { newVal ->
                            viewModel.onIntensityChange(6, newVal)
                            viewModel.setSelectedChannel(6)
                            redViewModel.setRed(newVal > 0)
                        },
                        onSelect = { viewModel.setSelectedChannel(6) },
                        enabled = !uiState.isPlaying
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0C0C0C))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DURATION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.durationMs.toInt()}ms", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Slider(
                    value = uiState.durationMs,
                    onValueChange = viewModel::onDurationChange,
                    valueRange = 100f..2000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF222222)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::addStep,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                if (uiState.currentSequenceSteps.isNotEmpty()) {
                    IconButton(
                        onClick = viewModel::clearSequence,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Right Column: Timeline (Switched to Vertical for Landscape V1 mapping)
        DraggableTimeline(uiState, viewModel, Modifier.weight(1f))
    }
}
