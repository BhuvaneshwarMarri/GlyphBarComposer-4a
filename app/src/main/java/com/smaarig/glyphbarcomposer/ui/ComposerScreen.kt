package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel
import kotlin.math.roundToInt

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
            .background(Color.Black)
            .padding(
                start = 16.dp,
                top = if (isLandscape) 12.dp else 16.dp,
                end = 16.dp,
                bottom = 0.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            }

            if (uiState.currentSequenceSteps.isNotEmpty()) {
                IconButton(onClick = viewModel::clearSequence, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteSweep, "Clear", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Column 1: Glyph Visualizer (6 white + 1 red)
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight(0.75f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("GLYPH", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))

                repeat(7) { index ->
                    val isRed = index == 6
                    val isSelected = uiState.selectedChannelIndex == index
                    val intensity = uiState.glyphIntensities[index]

                    if (isRed) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(40.dp),
                            thickness = 1.dp,
                            color = Color(0xFF2A2A2A)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    val finalColor = if (isRed) {
                        if (intensity > 0 || (isSelected && isRedOn)) Color(0xFFFF1744) else Color(0xFF1C1C1C)
                    } else {
                        intensityColor[intensity]
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isRed) 40.dp else 36.dp)
                            .clip(if (isRed) CircleShape else RoundedCornerShape(8.dp))
                            .background(finalColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF2A2A2A),
                                shape = if (isRed) CircleShape else RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setSelectedChannel(index) }
                    )

                    if (!isRed) Spacer(Modifier.height(10.dp))
                }
            }

            // Column 2: Sliders & Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("CONTROLS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Intensity Slider
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("LIGHT", color = Color.Gray, fontSize = 8.sp)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Slider(
                                value = uiState.glyphIntensities[uiState.selectedChannelIndex].toFloat(),
                                onValueChange = { viewModel.onIntensityChangeForSelected(it.toInt()) },
                                valueRange = 0f..3f,
                                steps = 2,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White
                                ),
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = -90f }
                                    .requiredWidth(200.dp)
                            )
                        }
                    }

                    // Duration Slider
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("${uiState.durationMs.toInt()}ms", color = Color.Gray, fontSize = 8.sp)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Slider(
                                value = uiState.durationMs,
                                onValueChange = viewModel::onDurationChange,
                                valueRange = 100f..2000f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White
                                ),
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = -90f }
                                    .requiredWidth(320.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = viewModel::addStep,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }

            // Column 3: Draggable Timeline
            DraggableTimeline(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1.2f).fillMaxHeight(0.75f)
            )
        }
    }
}

@Composable
fun DraggableTimeline(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight(0.75f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0C0C))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("SEQUENCE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.currentSequenceSteps.isEmpty()) {
                EmptyTimelinePlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(uiState.currentSequenceSteps) { index, step ->
                        val isDragging = draggingIndex == index
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    alpha = if (isDragging) 0.8f else 1f
                                    scaleX = if (isDragging) 1.05f else 1f
                                    scaleY = if (isDragging) 1.05f else 1f
                                }
                                .pointerInput(index) {
                                    detectDragGestures(
                                        onDragStart = { draggingIndex = index },
                                        onDragEnd = {
                                            // Handle drop reorder or delete
                                            if (dragOffsetY > 120f) {
                                                viewModel.removeStep(index)
                                            } else {
                                                val targetIndex = (index + (dragOffsetY / 65).roundToInt())
                                                    .coerceIn(0, uiState.currentSequenceSteps.size - 1)
                                                if (targetIndex != index) {
                                                    viewModel.reorderSteps(index, targetIndex)
                                                }
                                            }
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                        }
                                    )
                                }
                        ) {
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
        }

        // Trash Icon at bottom
        AnimatedVisibility(visible = draggingIndex != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (dragOffsetY > 120f) Color(0x33FF5252) else Color(0x1AFF5252)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = if (dragOffsetY > 120f) Color(0xFFFF5252) else Color.Gray,
                    modifier = Modifier.size(if (dragOffsetY > 120f) 28.dp else 22.dp)
                )
            }
        }

        if (uiState.currentSequenceSteps.isNotEmpty() && draggingIndex == null) {
            // Play/Save buttons at bottom of timeline
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (uiState.isPlaying) viewModel.stopPlayback()
                        else viewModel.startPlayback(uiState.currentSequenceSteps)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = viewModel::savePlaylist,
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun EmptyTimelinePlaceholder() {
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
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { showMenu = !showMenu },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("STEP", fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Black)
                Text("${step.durationMs}ms", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { i ->
                    val intensityVal = step.channelIntensities[getChannelForIndex(i)] ?: 0
                    val finalIntensity = if (i == 6 && intensityVal > 0) 6 else intensityVal
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 20.dp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { onLoad(); showMenu = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onInsertAfter(); showMenu = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.AddCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDelete(); showMenu = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}