package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.theme.intensityColor

@Composable
fun GlyphSquareButton(
    index: Int,
    intensity: Int,
    isSelected: Boolean,
    isRed: Boolean,
    onIntensityChange: (Int) -> Unit,
    onSelect: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val states = if (isRed) listOf(0, 3) else listOf(0, 1, 2, 3)

    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    val colorIdx = if (isRed && intensity > 0) 6 else intensity

    val ledColor = intensityColor[colorIdx]

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        label = "glyph_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF080808))
            .border(
                width = 1.dp,
                color = if (isSelected)
                    Color(0xFFFF4444)
                else
                    Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(2.dp)
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()

                        accumulatedDrag += dragAmount

                        if (accumulatedDrag > 35f) {
                            val currentIdx = states.indexOf(intensity)
                            val nextIdx =
                                (currentIdx - 1).coerceIn(0, states.size - 1)

                            if (nextIdx != currentIdx) {
                                onIntensityChange(states[nextIdx])
                            }

                            accumulatedDrag = 0f
                        } else if (accumulatedDrag < -35f) {
                            val currentIdx = states.indexOf(intensity)
                            val nextIdx =
                                (currentIdx + 1).coerceIn(0, states.size - 1)

                            if (nextIdx != currentIdx) {
                                onIntensityChange(states[nextIdx])
                            }

                            accumulatedDrag = 0f
                        }
                    },
                    onDragEnd = {
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                    }
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

        // Glow Layer
        if (intensity > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .blur(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        ledColor.copy(
                            alpha = when (intensity) {
                                1 -> 0.18f
                                2 -> 0.30f
                                else -> 0.45f
                            }
                        )
                    )
            )
        }

        // LED Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    ledColor.copy(
                        alpha = when (intensity) {
                            0 -> 0.05f
                            1 -> 0.35f
                            2 -> 0.65f
                            else -> 1f
                        }
                    )
                )
        )

        // Intensity Indicators
        if (!isRed) {

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                repeat(3) { level ->

                    Box(
                        modifier = Modifier
                            .size(
                                width = 10.dp,
                                height = 1.5.dp
                            )
                            .background(
                                if (intensity >= level + 1)
                                    Color.White.copy(alpha = 0.75f)
                                else
                                    Color.White.copy(alpha = 0.08f)
                            )
                    )
                }
            }

        } else if (intensity > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(width = 10.dp, height = 1.5.dp)
                    .background(Color.White.copy(alpha = 0.75f))
            )
        }
    }
}