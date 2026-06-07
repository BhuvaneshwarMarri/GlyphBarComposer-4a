package com.smaarig.glyphbarcomposer.ui.composer.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.theme.intensityColor

@Composable
fun GlyphScrollPicker(
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isRed: Boolean = false,
    enabled: Boolean = true
) {
    val states = if (isRed) listOf(0, 6) else listOf(0, 1, 2, 3)
    val infiniteCount = 10000
    val startOffset = (infiniteCount / 2)
    val initialIdx = startOffset + states.indexOf(intensity).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)

    val currentIntensity by rememberUpdatedState(intensity)
    val currentOnIntensityChange by rememberUpdatedState(onIntensityChange)

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    // Sync selection when user touches/drags
    LaunchedEffect(isDragged) {
        if (isDragged) {
            currentOnIntensityChange(currentIntensity)
        }
    }

    // Sync state TO ViewModel when user scrolls
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx ->
                val newVal = states[idx % states.size]
                if (newVal != currentIntensity) {
                    // Slight delay to avoid rapid updates during fast scrolls
                    kotlinx.coroutines.delay(10)
                    currentOnIntensityChange(newVal)
                }
            }
    }

    // Force sync FROM ViewModel when intensity is externally reset (e.g. Power Off or step load)
    LaunchedEffect(intensity) {
        if (!listState.isScrollInProgress) {
            val targetIdxInStates = states.indexOf(intensity)
            if (targetIdxInStates != -1) {
                val currentIdx = listState.firstVisibleItemIndex
                val currentIdxInStates = currentIdx % states.size
                if (currentIdxInStates != targetIdxInStates) {
                    var diff = targetIdxInStates - currentIdxInStates
                    // Handle wrapping in infinite list for shortest path
                    if (diff > states.size / 2) diff -= states.size
                    else if (diff < -states.size / 2) diff += states.size

                    if (intensity == 0) {
                        listState.scrollToItem(currentIdx + diff)
                    } else {
                        listState.animateScrollToItem(currentIdx + diff)
                    }
                }
            }
        }
    }

    val cellWidth = 54.dp

    Box(
        modifier = modifier
            .width(cellWidth + 16.dp) // Extra width for side indicators
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Scroll indicators (dotted texture)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = 0.8.dp.toPx()
            val dotSpacing = 5.dp.toPx()
            val startX = 6.dp.toPx()
            val endX = size.width - 6.dp.toPx()

            // Draw 7 dots to match the 7 glyphs theme
            for (i in 0 until 7) {
                val y = (size.height / 2) - (3 * dotSpacing) + (i * dotSpacing)
                drawCircle(Color(0xFF444444), radius = dotRadius, center = Offset(startX, y))
                drawCircle(Color(0xFF444444), radius = dotRadius, center = Offset(endX, y))
            }
        }

        Box(
            modifier = Modifier
                .width(cellWidth)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111111))
                .border(
                    width = 1.dp,
                    color = if (enabled) Color(0xFF3A3A3A) else Color(0xFF222222),
                    shape = RoundedCornerShape(8.dp)
                ),
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

                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(intensityColor[level])
                        )
                    }
                }
            }
        }
    }
}
