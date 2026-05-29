package com.smaarig.glyphbarcomposer.ui.composer.components.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.intensityColor

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
