package com.smaarig.glyphbarcomposer.ui.studio

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioTimelineStateTest {

    private val horizontalScrollState = mockk<ScrollState>()
    private val verticalScrollState = mockk<ScrollState>()
    private val density = mockk<Density>()
    private val pxPerMs = 0.2f
    
    private val timelineState = StudioTimelineState(
        horizontalScrollState = horizontalScrollState,
        verticalScrollState = verticalScrollState,
        initialPxPerMs = pxPerMs,
        density = density
    )

    @Test
    fun timeToDp_returnsCorrectValue() {
        val timeMs = 1000L
        val expectedDp = (timeMs * pxPerMs).dp
        val result = timelineState.timeToDp(timeMs)
        assertEquals(expectedDp, result)
    }

    @Test
    fun timeToPx_returnsCorrectValue() {
        val timeMs = 1000L
        val dpValue = (timeMs * pxPerMs).dp
        val expectedPx = 200f
        
        every { with(density) { any<androidx.compose.ui.unit.Dp>().toPx() } } returns expectedPx
        
        val result = timelineState.timeToPx(timeMs)
        assertEquals(expectedPx, result, 0.01f)
    }

    @Test
    fun pxToTime_returnsCorrectValue() {
        val px = 200f
        val physPxPerMs = 1.0f // 1.dp = 5px, 5px * 0.2 = 1.0
        
        every { with(density) { 1.dp.toPx() } } returns 5f
        
        val result = timelineState.pxToTime(px)
        assertEquals(200L, result)
    }

    @Test
    fun dragDeltaToMs_returnsCorrectValue() {
        val dragPx = 50f
        every { with(density) { 1.dp.toPx() } } returns 5f
        
        val result = timelineState.dragDeltaToMs(dragPx)
        assertEquals(50L, result)
    }
}
