package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.ui.getChannelForIndex
import com.smaarig.glyphbarcomposer.ui.studio.StudioTimelineState

@Composable
fun SequenceTrack(
    events: List<MusicStudioEvent>,
    state: StudioTimelineState,
    selectedEventId: Long?,
    onSelectEvent: (MusicStudioEvent?) -> Unit,
    onMoveEvent: (MusicStudioEvent, Long) -> Unit,
    onResizeEvent: (MusicStudioEvent, Long, Int) -> Unit,
    onDeleteEvent: (MusicStudioEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Optimized visible events calculation using derivedStateOf
    val visibleEvents by remember(events, state) {
        derivedStateOf {
            val scrollValue = state.horizontalScrollState.value
            val viewportWidth = state.horizontalScrollState.viewportSize
            
            val startTimeMs = state.pxToTime(scrollValue.toFloat())
            val endTimeMs = state.pxToTime((scrollValue + viewportWidth).toFloat())

            events.filter { event ->
                val eventEndMs = event.timestampMs + event.durationMs
                eventEndMs >= startTimeMs && event.timestampMs <= endTimeMs
            }
        }
    }
    
    val visibleTimeRange by remember(state) {
        derivedStateOf {
            val scrollValue = state.horizontalScrollState.value
            val viewportWidth = state.horizontalScrollState.viewportSize
            val startTimeMs = state.pxToTime(scrollValue.toFloat())
            val endTimeMs = state.pxToTime((scrollValue + viewportWidth).toFloat())
            startTimeMs..endTimeMs
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(state) {
                detectTapGestures { onSelectEvent(null) }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val msPerGrid = 500L
            val pxPerGrid = state.timeToPx(msPerGrid)
            
            // Only draw grid lines that are visible
            val startGrid = (visibleTimeRange.start / msPerGrid).toInt()
            val endGrid = (visibleTimeRange.endInclusive / msPerGrid).toInt() + 1
            
            for (i in startGrid..endGrid) {
                val x = i * pxPerGrid
                drawLine(Color(0xFF151515), Offset(x, 0f), Offset(x, size.height), 1f)
            }
        }

        visibleEvents.forEach { event ->
            key(event.id) {
                SequenceBlock(
                    event = event,
                    state = state,
                    isSelected = event.id == selectedEventId,
                    onClick = { onSelectEvent(event) },
                    onMove = { delta -> onMoveEvent(event, (event.timestampMs + delta).coerceAtLeast(0L)) },
                    onResizeLeft = { delta ->
                        val start = (event.timestampMs + delta).coerceAtLeast(0L)
                        val dur = (event.timestampMs + event.durationMs - start).toInt().coerceAtLeast(50)
                        onResizeEvent(event, start, dur)
                    },
                    onResizeRight = { delta ->
                        onResizeEvent(event, event.timestampMs, (event.durationMs + delta).toInt().coerceAtLeast(50))
                    },
                    onDelete = { onDeleteEvent(event) }
                )
            }
        }
    }
}

@Composable
fun SequenceBlock(
    event: MusicStudioEvent,
    state: StudioTimelineState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMove: (Long) -> Unit,
    onResizeLeft: (Long) -> Unit,
    onResizeRight: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var moveDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var resizeLeftDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var resizeRightDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var isDragging by remember(event.id) { mutableStateOf(false) }
    var isResizing by remember(event.id) { mutableStateOf(false) }

    val currentTimestampMs = (event.timestampMs + moveDeltaMs + resizeLeftDeltaMs).coerceAtLeast(0L)
    val currentDurationMs = (event.durationMs + resizeRightDeltaMs - resizeLeftDeltaMs).coerceAtLeast(50)

    val x = state.timeToDp(currentTimestampMs)
    val widthDp = state.timeToDp(currentDurationMs).coerceAtLeast(MIN_BLOCK_WIDTH)

    Box(
        modifier = Modifier
            .offset(x = x)
            .width(widthDp)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .testTag("SequenceBlock_${event.id}")
            .graphicsLayer {
                if (isDragging || isResizing) {
                    scaleX = 1.02f
                    scaleY = 1.05f
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RESIZE_HANDLE_WIDTH)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFF00C853).copy(0.2f) else Color(0xFF161616))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF00C853) else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(event.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onDelete() }
                    )
                }
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isDragging = true; onClick() },
                        onDragEnd = {
                            isDragging = false
                            val totalDelta = moveDeltaMs
                            val snappedDelta = Math.round(totalDelta / 25.0) * 25
                            onMove(snappedDelta)
                            moveDeltaMs = 0L
                        },
                        onDragCancel = { isDragging = false; moveDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        moveDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        ) {
            if (isDragging) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
            
            Column(
                modifier = Modifier.align(Alignment.Center).padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                repeat(7) { i -> PatternDot(event, i) }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(RESIZE_HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(Color.White.copy(0.05f))
                .testTag("ResizeHandleLeft_${event.id}")
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isResizing = true; onClick() },
                        onDragEnd = { 
                            isResizing = false
                            val snapped = Math.round(resizeLeftDeltaMs / 25.0) * 25
                            onResizeLeft(snapped)
                            resizeLeftDeltaMs = 0L 
                        },
                        onDragCancel = { isResizing = false; resizeLeftDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        resizeLeftDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(RESIZE_HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(Color.White.copy(0.05f))
                .testTag("ResizeHandleRight_${event.id}")
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isResizing = true; onClick() },
                        onDragEnd = { 
                            isResizing = false
                            val snapped = Math.round(resizeRightDeltaMs / 25.0) * 25
                            onResizeRight(snapped)
                            resizeRightDeltaMs = 0L 
                        },
                        onDragCancel = { isResizing = false; resizeRightDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        resizeRightDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        )
    }
}

@Composable
fun PatternDot(event: MusicStudioEvent, index: Int) {
    val ch = getChannelForIndex(index)
    val intensity = event.channelIntensities[ch] ?: 0
    val isRed = index == 6
    Box(
        modifier = Modifier
            .size(if (intensity > 0) 4.dp else 2.dp)
            .clip(CircleShape)
            .background(
                if (intensity > 0) {
                    if (isRed) Color(0xFFFF1744) else Color.White.copy(alpha = (intensity / 3f).coerceIn(0.4f, 1.0f))
                } else Color(0xFF2A2A2A)
            )
    )
}
