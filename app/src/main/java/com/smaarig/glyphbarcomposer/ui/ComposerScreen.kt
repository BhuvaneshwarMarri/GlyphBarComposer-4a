package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel
import kotlin.math.roundToInt

// ─── Horizontal snap-scroll intensity picker for a single glyph ─────────────
@Composable
fun GlyphScrollPicker(
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    isRed: Boolean = false,
    enabled: Boolean = true
) {
    val states = if (isRed) listOf(0, 3) else listOf(0, 1, 2, 3)
    val infiniteCount = 10000
    val startOffset = (infiniteCount / 2) - ((infiniteCount / 2) % states.size)
    val initialIdx = startOffset + states.indexOf(intensity).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)

    LaunchedEffect(intensity) {
        val targetIdxInStates = states.indexOf(intensity)
        if (targetIdxInStates != -1) {
            val currentIdx = listState.firstVisibleItemIndex
            val currentIdxInStates = currentIdx % states.size
            if (currentIdxInStates != targetIdxInStates) {
                var diff = targetIdxInStates - currentIdxInStates
                if (diff > states.size / 2) diff -= states.size
                else if (diff < -states.size / 2) diff += states.size
                listState.animateScrollToItem(currentIdx + diff)
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val settled = listState.firstVisibleItemIndex
            val newVal = states[settled % states.size]
            if (newVal != intensity) onIntensityChange(newVal)
        }
    }

    val cellWidth = 54.dp

    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            userScrollEnabled = enabled
        ) {
            items(infiniteCount) { idx ->
                val level = states[idx % states.size]
                val colorIdx = if (isRed && level > 0) 6 else level

                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(intensityColor[colorIdx])
                    )
                }
            }
        }
    }
}

// ─── Composer screen ─────────────────────────────────────────────────────────
@Composable
fun ComposerScreen(
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier,
    redViewModel: RedGlyphViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPOSER",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.currentSequenceSteps.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearSequence, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteSweep, "Clear",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.turnOffAllGlyphs()
                        redViewModel.setRed(false)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew, "Turn Off All",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Column 1: Glyph scroll-pickers ──────────────────────────────
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight(0.8f)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "GLYPH",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))

                repeat(7) { index ->
                    val isRed = index == 6
                    val isSelected = uiState.selectedChannelIndex == index
                    val intensity = uiState.glyphIntensities[index]

                    if (isRed) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(50.dp),
                            thickness = 1.dp,
                            color = Color(0xFF2A2A2A)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.Transparent)
                        )

                        GlyphScrollPicker(
                            intensity = intensity,
                            onIntensityChange = { newVal ->
                                viewModel.onIntensityChange(index, newVal)
                                viewModel.setSelectedChannel(index)
                                if (isRed) redViewModel.setRed(newVal > 0)
                            },
                            isRed = isRed,
                            enabled = !uiState.isPlaying
                        )
                    }

                    if (!isRed) Spacer(Modifier.height(8.dp))
                }
            }

            // ── Column 2: Duration slider + Add Step ────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "CONTROLS",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${uiState.durationMs.toInt()}ms", color = Color.Gray, fontSize = 8.sp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
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
                                .requiredWidth(370.dp)
                        )
                    }
                }

                Button(
                    onClick = viewModel::addStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }

            // ── Column 3: Draggable Timeline ────────────────────────────────
            DraggableTimeline(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

// ─── Draggable timeline ───────────────────────────────────────────────────────
@Composable
fun DraggableTimeline(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
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

    // Approximate px per step item (55dp card + 6dp gap = 61dp @ ~3x density ≈ 183px)
    val itemHeightPx = 183f

    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
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
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    alpha = if (isDragging) 0.78f else 1f
                                    scaleX = if (isDragging) 1.03f else 1f
                                    scaleY = if (isDragging) 1.03f else 1f
                                }
                                .pointerInput(index) {
                                    detectDragGestures(
                                        onDragStart = {
                                            if (!uiState.isPlaying) draggingIndex = index
                                        },
                                        onDragEnd = {
                                            val src = draggingIndex
                                            if (src != null) {
                                                // Pure reorder only — deletion is exclusively via
                                                // the delete icon inside StepPreviewBox
                                                val steps = (dragOffsetY / itemHeightPx).roundToInt()
                                                val dst = (src + steps)
                                                    .coerceIn(0, uiState.currentSequenceSteps.size - 1)
                                                if (dst != src) viewModel.reorderSteps(src, dst)
                                            }
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            if (!uiState.isPlaying) dragOffsetY += amount.y
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

        // Play / Save — shown only when steps exist and nothing is being dragged
        if (uiState.currentSequenceSteps.isNotEmpty() && draggingIndex == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
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
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Empty timeline placeholder ───────────────────────────────────────────────
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

// ─── Step preview card ────────────────────────────────────────────────────────
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
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
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