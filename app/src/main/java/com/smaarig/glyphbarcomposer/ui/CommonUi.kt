package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Intensity palette ──────────────────────────────────────────────────────
val intensityColor = listOf(
    Color(0xFF1C1C1C),   // 0 – OFF
    Color(0xFF686868),   // 1 – LOW
    Color(0xFFCDCDCD),   // 2 – MED
    Color(0xFFFFFFFF),   // 3 – HIGH
    Color(0xFFC62828),   // 4 – RED (Low)
    Color(0xFFEF5350),   // 5 – RED (Med)
    Color(0xFFFF1744)    // 6 – RED (Full)
)

val intensityBorder = listOf(
    Color(0xFF3A3A3A),   // 0
    Color(0xFF888888),   // 1
    Color(0xFFE0E0E0),   // 2
    Color(0xFFFFFFFF),   // 3
    Color(0xFF5A1010),   // 4 - Red Border
    Color(0xFF8E2A2A),   // 5 - Red Border
    Color(0xFFF44336)    // 6 - Red Border
)

fun labelColor(intensity: Int) =
    if (intensity >= 2) Color(0xFF111111) else Color(0xFFFFFFFF)

val statusLabel = listOf("", "LOW", "MED", "HIGH", "ON", "ON", "ON")

@Composable
fun GlyphBox(
    label: String,
    intensity: Int,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit,
    enabled: Boolean = true,
    isRed: Boolean = false
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (intensity > 0) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val fillColor by animateColorAsState(
        targetValue = intensityColor[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "fill"
    )
    val borderColor by animateColorAsState(
        targetValue = intensityBorder[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "border"
    )

    val dragProgress = (kotlin.math.abs(accumulatedDrag).coerceAtMost(30f) / 30f)
    val dragOverlayAlpha = dragProgress * 0.18f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(fillColor)
            .then(if (enabled) {
                Modifier
                    .pointerInput(intensity) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (isRed) return@detectVerticalDragGestures
                                change.consume()
                                accumulatedDrag += dragAmount
                                val threshold = 28f
                                if (accumulatedDrag > threshold) {
                                    if (intensity > 0) onIntensityChange(intensity - 1)
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag < -threshold) {
                                    if (intensity < 3) onIntensityChange(intensity + 1)
                                    accumulatedDrag = 0f
                                }
                            },
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f }
                        )
                    }
                    .clickable {
                        if (isRed) {
                            if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
                        } else {
                            if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
                        }
                    }
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dragOverlayAlpha > 0f && enabled) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (accumulatedDrag < 0) Color.White.copy(alpha = dragOverlayAlpha)
                        else Color.Black.copy(alpha = dragOverlayAlpha)
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor(intensity)
            )
            if (intensity > 0) {
                Text(
                    text = statusLabel[intensity],
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor(intensity).copy(alpha = 0.75f),
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
}

fun getChannelForIndex(index: Int): Int = when (index) {
    0 -> com.nothing.ketchum.Glyph.Code_25111.A_1
    1 -> com.nothing.ketchum.Glyph.Code_25111.A_2
    2 -> com.nothing.ketchum.Glyph.Code_25111.A_3
    3 -> com.nothing.ketchum.Glyph.Code_25111.A_4
    4 -> com.nothing.ketchum.Glyph.Code_25111.A_5
    5 -> com.nothing.ketchum.Glyph.Code_25111.A_6
    6 -> com.nothing.ketchum.Glyph.Code_22111.E1
    else -> 0
}
