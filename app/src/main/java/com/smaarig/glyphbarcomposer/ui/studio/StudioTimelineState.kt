package com.smaarig.glyphbarcomposer.ui.studio

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Encapsulates all coordinate-mapping logic for the DAW Studio Timeline.
 */
@Stable
class StudioTimelineState(
    val horizontalScrollState: ScrollState,
    val verticalScrollState: ScrollState,
    initialPxPerMs: Float = 0.18f,
    val density: Density
) {
    var pxPerMs by mutableStateOf(initialPxPerMs)
    
    val scrollState: ScrollState get() = horizontalScrollState

    /** Logical Dp offset for a given timestamp. Used for layout offsets/widths. */
    fun timeToDp(timeMs: Long): Dp = (timeMs * pxPerMs).dp

    /** Physical screen-pixel offset — used for scrollState.animateScrollTo(). */
    fun timeToPx(timeMs: Long): Float = with(density) { timeToDp(timeMs).toPx() }

    /** Physical screen-pixel offset to time in ms. */
    fun pxToTime(px: Float): Long {
        val physPxPerMs = with(density) { (1.dp).toPx() } * pxPerMs
        return (px / physPxPerMs).toLong()
    }

    /**
     * Converts a raw gesture DELTA (dragAmount.x from detectDragGestures) to ms.
     */
    fun dragDeltaToMs(dragPx: Float): Long {
        val physPxPerMs = with(density) { (1.dp).toPx() } * pxPerMs
        return (dragPx / physPxPerMs).toLong()
    }
}

@Composable
fun rememberStudioTimelineState(
    horizontalScrollState: ScrollState = rememberScrollState(),
    verticalScrollState: ScrollState = rememberScrollState(),
    pxPerMs: Float = 0.18f,
    density: Density = androidx.compose.ui.platform.LocalDensity.current
): StudioTimelineState = remember(horizontalScrollState, verticalScrollState, density) {
    StudioTimelineState(horizontalScrollState, verticalScrollState, pxPerMs, density)
}
