package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.draw.scale
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
    val startOffset = (infiniteCount / 2)
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
    redViewModel: RedGlyphViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var useOldVersion by remember { mutableStateOf(false) }

    var isPowerOffAnimating by remember { mutableStateOf(false) }
    val powerScale by animateFloatAsState(
        targetValue = if (isPowerOffAnimating) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { isPowerOffAnimating = false },
        label = "powerOffScale"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ComposerLandscape(
            uiState = uiState,
            viewModel = viewModel,
            redViewModel = redViewModel,
            useOldVersion = useOldVersion,
            onToggleVersion = { useOldVersion = it },
            powerScale = powerScale,
            onPowerClick = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            }
        )
    } else {
        ComposerPortrait(
            uiState = uiState,
            viewModel = viewModel,
            redViewModel = redViewModel,
            useOldVersion = useOldVersion,
            onToggleVersion = { useOldVersion = it },
            powerScale = powerScale,
            onPowerClick = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            }
        )
    }
}

@Composable
fun ComposerPortrait(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel,
    useOldVersion: Boolean,
    onToggleVersion: (Boolean) -> Unit,
    powerScale: Float,
    onPowerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComposerHeader(
            uiState = uiState,
            viewModel = viewModel,
            useOldVersion = useOldVersion,
            onToggleVersion = onToggleVersion,
            powerScale = powerScale,
            onPowerClick = onPowerClick
        )

        if (useOldVersion) {
            ComposerScreenOld(uiState, viewModel, redViewModel)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlsColumn(uiState, viewModel, Modifier.weight(1f))
                GlyphsColumn(uiState, viewModel, redViewModel, Modifier.width(88.dp))
                DraggableTimeline(uiState, viewModel, Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
fun ComposerLandscape(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel,
    useOldVersion: Boolean,
    onToggleVersion: (Boolean) -> Unit,
    powerScale: Float,
    onPowerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComposerHeader(
            uiState = uiState,
            viewModel = viewModel,
            useOldVersion = useOldVersion,
            onToggleVersion = onToggleVersion,
            powerScale = powerScale,
            onPowerClick = onPowerClick
        )

        if (useOldVersion) {
            ComposerScreenOldLandscape(uiState, viewModel, redViewModel)
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Horizontal Glyphs & Horizontal Controls
                Column(
                    modifier = Modifier.weight(1.3f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LandscapeGlyphsRow(uiState, viewModel, redViewModel, Modifier.weight(1f))
                    LandscapeControlsRow(uiState, viewModel)
                }

                // Right Panel: Timeline
                DraggableTimeline(uiState, viewModel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComposerHeader(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    useOldVersion: Boolean,
    onToggleVersion: (Boolean) -> Unit,
    powerScale: Float,
    onPowerClick: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("V2" to false, "V1" to true).forEach { (label, isV1) ->
                        val selected = useOldVersion == isV1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) {
                                        if (isV1) Color(0xFFFFEB3B) else Color(0xFFFFC1CC)
                                    } else Color.Transparent
                                )
                                .clickable { onToggleVersion(isV1) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else Color(0xFF555555),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                            )
                        }
                    }
                }
            }

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
                onClick = onPowerClick,
                modifier = Modifier
                    .size(32.dp)
                    .scale(powerScale)
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew, "Turn Off All",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─── Portrait Specific V2 Layout Components ──────────────────────────────────
@Composable
private fun ControlsColumn(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CONTROLS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)

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
                        .requiredWidth(320.dp)
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
}

@Composable
private fun GlyphsColumn(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GLYPH", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        repeat(7) { index ->
            val isRed = index == 6
            val isSelected = uiState.selectedChannelIndex == index
            val intensity = uiState.glyphIntensities[index]

            if (isRed) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(Modifier.width(4.dp))
                    HorizontalDivider(
                        modifier = Modifier.width(54.dp),
                        thickness = 1.dp,
                        color = Color(0xFF2A2A2A)
                    )
                }
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
}

// ─── Landscape Specific V2 Layout Components ─────────────────────────────────
@Composable
private fun LandscapeGlyphsRow(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GLYPHS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(7) { index ->
                val isRed = index == 6
                val isSelected = uiState.selectedChannelIndex == index
                val intensity = uiState.glyphIntensities[index]

                if (isRed) {
                    Box(modifier = Modifier.width(1.dp).height(44.dp).background(Color(0xFF2A2A2A)))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
            }
        }
    }
}

@Composable
private fun LandscapeControlsRow(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                )
            )
        }

        Button(
            onClick = viewModel::addStep,
            modifier = Modifier
                .height(44.dp)
                .width(120.dp),
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
                                enabled = !uiState.isPlaying
                            )
                        }
                    }
                }
            }
        }

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
fun OldGlyphButton(
    index: Int,
    intensity: Int,
    isSelected: Boolean,
    isRed: Boolean,
    onIntensityChange: (Int) -> Unit,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    val states = if (isRed) listOf(0, 3) else listOf(0, 1, 2, 3)
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val colorIdx = if (isRed && intensity > 0) 6 else intensity

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isRed) "R" else "${index + 1}",
            color = if (isSelected) Color.White else Color(0xFF444444),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) Color(0xFF1E1E1E) else Color(0xFF111111)
                )
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Color.White else Color(0xFF222222),
                    shape = RoundedCornerShape(10.dp)
                )
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                            if (accumulatedDrag > 40f) {
                                val currentIdx = states.indexOf(intensity)
                                val nextIdx = (currentIdx - 1).coerceIn(0, states.size - 1)
                                if (nextIdx != currentIdx) onIntensityChange(states[nextIdx])
                                accumulatedDrag = 0f
                            } else if (accumulatedDrag < -40f) {
                                val currentIdx = states.indexOf(intensity)
                                val nextIdx = (currentIdx + 1).coerceIn(0, states.size - 1)
                                if (nextIdx != currentIdx) onIntensityChange(states[nextIdx])
                                accumulatedDrag = 0f
                            }
                        },
                        onDragEnd = { accumulatedDrag = 0f },
                        onDragCancel = { accumulatedDrag = 0f }
                    )
                }
                .clickable(enabled = enabled) {
                    val currentIdx = states.indexOf(intensity)
                    val nextIdx = (currentIdx + 1) % states.size
                    onIntensityChange(states[nextIdx])
                    onSelect()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(intensityColor[colorIdx])
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val maxStates = if (isRed) 1 else 3
                repeat(maxStates) { dotIdx ->
                    val dotLevel = if (isRed) 3 else (dotIdx + 1)
                    val active = intensity >= dotLevel && intensity > 0
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (active) Color.White.copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                    )
                }
            }
        }
    }
}

// ─── V1 Portrait Screen ──────────────────────────────────────────────────────
@Composable
fun ComposerScreenOld(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        .padding(bottom = 0.dp)
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
                Text(
                    "${uiState.durationMs.toInt()}ms",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TIMELINE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            DraggableTimelineHorizontal(uiState, viewModel, onSaveRequest = { showSaveDialog = true })
        }
    }
}

// ─── V1 Landscape Screen ─────────────────────────────────────────────────────
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

@Composable
fun StepPreviewBox(
    step: GlyphSequence,
    onDelete: () -> Unit,
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
                    IconButton(onClick = { onDelete(); showMenu = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}