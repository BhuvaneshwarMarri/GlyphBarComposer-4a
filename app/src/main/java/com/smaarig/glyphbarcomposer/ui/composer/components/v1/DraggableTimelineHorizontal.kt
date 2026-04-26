package com.smaarig.glyphbarcomposer.ui.composer.components.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.getChannelForIndex
import com.smaarig.glyphbarcomposer.ui.intensityColor
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

@Composable
fun DraggableTimelineHorizontal(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    onSaveRequest: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0C0C))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            if (uiState.currentSequenceSteps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Layers,
                            null,
                            tint = Color(0xFF222222),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No steps yet",
                            color = Color(0xFF333333),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.currentSequenceSteps) { index, step ->
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161616))
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                                .clickable(enabled = !uiState.isPlaying) { viewModel.loadStep(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "STEP ${index + 1}",
                                    color = Color(0xFF555555),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    "${step.durationMs}ms",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(7) { i ->
                                        val intensityVal = step.channelIntensities[getChannelForIndex(i)] ?: 0
                                        val finalIntensity = if (i == 6 && intensityVal > 0) 6 else intensityVal
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 14.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(intensityColor[finalIntensity])
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                IconButton(
                                    onClick = { viewModel.removeStep(index) },
                                    modifier = Modifier.size(20.dp),
                                    enabled = !uiState.isPlaying
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = Color(0xFF444444),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.currentSequenceSteps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (uiState.isPlaying) viewModel.stopPlayback()
                        else viewModel.startPlayback(uiState.currentSequenceSteps)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (uiState.isPlaying) Color(0xFF00E676) else Color.White,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onSaveRequest,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.Save,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
