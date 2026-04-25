package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComposerPanel(
    intensities: List<Int>,
    liveIntensities: List<Int>,
    isPlaying: Boolean,
    isReady: Boolean,
    isSelected: Boolean,
    defaultDuration: Int,
    onIntensityChange: (Int, Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onClear: () -> Unit,
    onInsert: () -> Unit
) {
    val anyActive = intensities.any { it > 0 }
    val canInsert = isReady && !isPlaying && anyActive
    val isEditable = !isPlaying || isSelected

    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isPlaying && !isSelected) "LIVE OUTPUT" else "GLYPH PAINTER",
                        color = if (isPlaying && !isSelected) Color(0xFF00C853) else Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                    Text(
                        if (isPlaying && !isSelected) "Responding in real-time"
                        else "Paint pattern & insert beat",
                        color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (anyActive && !isPlaying) {
                        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            GlyphComposerPanel(
                intensities = if (isPlaying && !isSelected) liveIntensities else intensities,
                onIntensityChange = onIntensityChange,
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable { 
                            val next = when(defaultDuration) {
                                100 -> 200; 200 -> 300; 300 -> 500; 500 -> 800; else -> 100
                            }
                            onDurationChange(next)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${defaultDuration}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onInsert,
                    enabled = canInsert,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1A1A1A), disabledContentColor = Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("INSERT", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun GlyphComposerPanel(
    intensities: List<Int>,
    onIntensityChange: (index: Int, newIntensity: Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080808))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        intensities.forEachIndexed { idx, intensity ->
            val color = when (idx) {
                0 -> Color(0xFFFFFFFF)
                1 -> Color(0xFF82AAFF)
                2 -> Color(0xFF89DDFF)
                3 -> Color(0xFFC3E88D)
                4 -> Color(0xFFFFCB6B)
                5 -> Color(0xFFB39DDB)
                else -> Color(0xFFFF1744)
            }
            if (idx < 6) {
                IntensityFader(
                    color = color,
                    intensity = intensity,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onIntensityChange = { onIntensityChange(idx, it) }
                )
            } else {
                RedFader(
                    color = color,
                    isOn = intensity > 0,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onToggle = { onIntensityChange(idx, if (it) 3 else 0) }
                )
            }
        }
    }
}

@Composable
fun IntensityFader(
    color: Color,
    intensity: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit
) {
    var trackHeightPx by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F0F0F))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                            val newLevel = when {
                                fraction > 0.8f -> 3
                                fraction > 0.45f -> 2
                                fraction > 0.15f -> 1
                                else -> 0
                            }
                            onIntensityChange(newLevel)
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val y = change.position.y
                        val fraction = 1f - (y / trackHeightPx).coerceIn(0f, 1f)
                        val newLevel = when {
                            fraction > 0.8f -> 3
                            fraction > 0.45f -> 2
                            fraction > 0.15f -> 1
                            else -> 0
                        }
                        if (newLevel != intensity) onIntensityChange(newLevel)
                    }
                }
        ) {
            val fillFraction by animateFloatAsState(
                targetValue = intensity / 3f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.4f), color)
                        )
                    )
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )

            // Visual markers
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(1.dp)
                            .background(if ((3-i) <= intensity && intensity > 0) Color.White.copy(0.4f) else Color.White.copy(0.05f))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (intensity > 0) color else Color(0xFF1A1A1A))
        )
    }
}

@Composable
fun RedFader(
    color: Color,
    isOn: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bgAlpha by animateFloatAsState(if (isOn) 0.3f else 0f)
        val borderAlpha by animateFloatAsState(if (isOn) 1f else 0.1f)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = bgAlpha))
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onToggle(!isOn) },
            contentAlignment = Alignment.Center
        ) {
            if (isOn) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "RED",
            color = if (isOn) color else Color(0xFF444444),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}
