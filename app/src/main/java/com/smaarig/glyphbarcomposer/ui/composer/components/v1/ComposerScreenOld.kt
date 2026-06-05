package com.smaarig.glyphbarcomposer.ui.composer.components.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.PlaybackState
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerScreenOld(
    glyphIntensities: List<Int>,
    currentSequenceSteps: List<GlyphSequence>,
    durationMs: Float,
    isPlaying: Boolean,
    isPaused: Boolean,
    selectedChannelIndex: Int,
    activePlaylistId: Long?,
    activePresetName: String?,
    onIntensityChange: (Int, Int) -> Unit,
    onDurationChange: (Float) -> Unit,
    onAddStep: () -> Unit,
    onRemoveStep: (Int) -> Unit,
    onClearSequence: () -> Unit,
    onLoadStep: (Int) -> Unit,
    onStartPlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onTogglePause: () -> Unit,
    onSavePlaylist: (String) -> Unit,
    onPlaySequence: (com.smaarig.glyphbarcomposer.data.PlaylistWithSteps) -> Unit,
    onDeletePlaylist: (com.smaarig.glyphbarcomposer.data.Playlist) -> Unit,
    onChannelSelect: (Int) -> Unit,
    onRedToggle: (Boolean) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    if (showSaveDialog) {
        com.smaarig.glyphbarcomposer.ui.StyledSaveDialog(
            title = "Save Sequence",
            value = fileName,
            onValueChange = { fileName = it },
            onSave = {
                if (fileName.isNotBlank()) {
                    onSavePlaylist(fileName)
                    showSaveDialog = false
                    fileName = ""
                }
            },
            onDismiss = { showSaveDialog = false },
            placeholder = "Sequence Name"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C0C0C))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
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
                        intensity = glyphIntensities[index],
                        isSelected = selectedChannelIndex == index,
                        isRed = false,
                        onIntensityChange = { newVal ->
                            onIntensityChange(index, newVal)
                            onChannelSelect(index)
                        },
                        onSelect = { onChannelSelect(index) },
                        enabled = !isPlaying
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = 0.dp)
                        .width(1.dp)
                        .height(52.dp)
                        .background(Color(0xFF222222))
                )

                OldGlyphButton(
                    index = 6,
                    intensity = glyphIntensities[6],
                    isSelected = selectedChannelIndex == 6,
                    isRed = true,
                    onIntensityChange = { newVal ->
                        onIntensityChange(6, newVal)
                        onChannelSelect(6)
                        onRedToggle(newVal > 0)
                    },
                    onSelect = { onChannelSelect(6) },
                    enabled = !isPlaying
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C0C0C))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DURATION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${durationMs.toInt()}ms",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Slider(
                value = durationMs,
                onValueChange = onDurationChange,
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
                onClick = onAddStep,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }

            if (currentSequenceSteps.isNotEmpty()) {
                IconButton(
                    onClick = onClearSequence,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TIMELINE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            DraggableTimelineHorizontal(
                steps = currentSequenceSteps,
                isPlaying = isPlaying,
                isPaused = isPaused,
                activePlaylistId = activePlaylistId,
                activePresetName = activePresetName,
                onRemoveStep = onRemoveStep,
                onLoadStep = onLoadStep,
                onStartPlayback = onStartPlayback,
                onStopPlayback = onStopPlayback,
                onSaveRequest = { showSaveDialog = true })
        }

        Spacer(Modifier.height(120.dp))
    }
}
