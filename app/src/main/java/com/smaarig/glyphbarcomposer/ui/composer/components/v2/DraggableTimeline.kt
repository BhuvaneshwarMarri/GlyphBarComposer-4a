package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.composer.components.common.EmptyTimelinePlaceholder
import com.smaarig.glyphbarcomposer.ui.composer.components.common.StepPreviewBox
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@kotlinx.coroutines.FlowPreview
@Composable
fun DraggableTimeline(
    steps: List<GlyphSequence>,
    isPlaying: Boolean,
    onRemoveStep: (Int) -> Unit,
    onReorderSteps: (from: Int, to: Int) -> Unit,
    onLoadStep: (Int) -> Unit,
    onStartPlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 94.dp.toPx() } } // 88dp + 6dp spacing

    var showSaveDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    // Sync hardware when scrolling manually
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, idx) -> idx }
            .distinctUntilChanged()
            .debounce(150L)
            .collect { idx ->
                if (!isPlaying && steps.isNotEmpty()) {
                    onLoadStep(idx.coerceIn(0, steps.size - 1))
                }
            }
    }

    // Auto-scroll to end when a new step is added
    LaunchedEffect(steps.size) {
        if (steps.isNotEmpty()) {
            listState.animateScrollToItem(steps.size - 1)
        }
    }

    if (showSaveDialog) {
        com.smaarig.glyphbarcomposer.ui.StyledSaveDialog(
            title = "Save Sequence",
            value = fileName,
            onValueChange = { fileName = it },
            onSave = {
                if (fileName.isNotBlank()) {
                    onSave(fileName)
                    showSaveDialog = false
                    fileName = ""
                }
            },
            onDismiss = { showSaveDialog = false },
            placeholder = "Sequence Name"
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF080808))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SEQUENCE",
            color = Color(0xFF666666),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Box(modifier = Modifier.weight(1f)) {
            if (steps.isEmpty()) {
                EmptyTimelinePlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("timeline_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(steps) { index, step ->
                        val isDragging = draggingIndex == index

                        val displacement = animateFloatAsState(
                            targetValue = when {
                                isDragging -> 0f
                                draggingIndex == null || targetIndex == null -> 0f
                                index in (targetIndex!! until draggingIndex!!) -> itemHeightPx
                                index in (draggingIndex!! + 1..targetIndex!!) -> -itemHeightPx
                                else -> 0f
                            },
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                            label = "item_displacement"
                        )

                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else displacement.value
                                    alpha = if (isDragging) 0.85f else 1f
                                    scaleX = if (isDragging) 1.04f else 1f
                                    scaleY = if (isDragging) 1.04f else 1f
                                }
                        ) {
                            StepPreviewBox(
                                step = step,
                                index = index,
                                onDelete = { onRemoveStep(index) },
                                onLoad = { onLoadStep(index) },
                                enabled = !isPlaying,
                                onDragStart = {
                                    if (!isPlaying) {
                                        draggingIndex = index
                                        targetIndex = index
                                    }
                                },
                                onDragEnd = {
                                    val src = draggingIndex
                                    val dest = targetIndex
                                    if (src != null && dest != null && src != dest) {
                                        onReorderSteps(src, dest)
                                    }
                                    draggingIndex = null
                                    targetIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    targetIndex = null
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (!isPlaying && draggingIndex != null) {
                                        dragOffsetY += amount.y

                                        // Calculate target index based on current drag position
                                        val newTargetIndex = (draggingIndex!! + (dragOffsetY / itemHeightPx).roundToInt())
                                            .coerceIn(0, steps.size - 1)

                                        if (newTargetIndex != targetIndex) {
                                            targetIndex = newTargetIndex
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (steps.isNotEmpty() && draggingIndex == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) onStopPlayback()
                        else onStartPlayback()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isPlaying) Color(0xFF00E676) else Color.White,
                            RoundedCornerShape(10.dp)
                        )
                        .testTag("play_button")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
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
                        .testTag("save_button")
                ) {
                    Icon(
                        Icons.Default.Save,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
