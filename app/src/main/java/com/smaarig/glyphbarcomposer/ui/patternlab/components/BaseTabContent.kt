package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BaseTabContent(
    title: String,
    name: String?,
    speed: Float,
    inverted: Boolean,
    mirrored: Boolean,
    reversed: Boolean,
    pingPong: Boolean,
    onPick: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onInvertChange: (Boolean) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onPingPongChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onPick() }) {
                        Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(name ?: "Select Sequence...", color = if (name != null) Color.White else Color(0xFF444444), fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabToggleButton(icon = Icons.Default.InvertColors, label = "Invert", active = inverted) { onInvertChange(!inverted) }
                    LabToggleButton(icon = Icons.Default.Flip, label = "Mirror", active = mirrored) { onMirrorChange(!mirrored) }
                    LabToggleButton(icon = Icons.AutoMirrored.Filled.Undo, label = "Reverse", active = reversed) { onReverseChange(!reversed) }
                    LabToggleButton(icon = Icons.Default.SyncAlt, label = "Ping-Pong", active = pingPong) { onPingPongChange(!pingPong) }
                }
            }
        }

        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabSliderRow("Speed", "${"%.1f".format(speed)}x", speed, 0.5f..2.0f, onValueChange = onSpeedChange)
            }
        }
    }
}
