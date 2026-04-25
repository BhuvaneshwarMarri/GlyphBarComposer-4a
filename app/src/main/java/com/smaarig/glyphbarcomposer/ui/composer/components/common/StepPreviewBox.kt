package com.smaarig.glyphbarcomposer.ui.composer.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.getChannelForIndex
import com.smaarig.glyphbarcomposer.ui.intensityBorder
import com.smaarig.glyphbarcomposer.ui.intensityColor

@Composable
fun StepPreviewBox(
    step: GlyphSequence,
    index: Int,
    onDelete: () -> Unit,
    onLoad: () -> Unit,
    enabled: Boolean,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { showMenu = !showMenu },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left part: Drag Handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = Color(0xFF333333),
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                            onDrag = { change, dragAmount -> onDrag(change, dragAmount) }
                        )
                    }
            )

            // Middle part: V1 style info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "STEP ${index + 1}",
                    color = Color(0xFF555555),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "${step.durationMs}ms",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                    repeat(7) { i ->
                        val intensityVal = step.channelIntensities[getChannelForIndex(i)] ?: 0
                        val finalIntensity = if (i == 6 && intensityVal > 0) 6 else intensityVal
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 16.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(intensityColor[finalIntensity])
                        )
                    }
                }
            }
            
            // Right part: Quick actions or indicator
            IconButton(
                onClick = onLoad,
                modifier = Modifier.size(32.dp),
                enabled = enabled
            ) {
                Icon(
                    Icons.Default.Refresh,
                    null,
                    tint = Color(0xFF444444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showMenu) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showMenu = false },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDelete(); showMenu = false },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF221111), CircleShape)
                            .border(1.dp, Color(0xFF442222), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
