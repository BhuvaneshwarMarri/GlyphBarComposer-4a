package com.smaarig.glyphbarcomposer.ui.composer.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.intensityColor

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
