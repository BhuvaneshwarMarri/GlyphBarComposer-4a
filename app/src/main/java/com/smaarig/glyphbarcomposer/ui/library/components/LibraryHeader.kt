package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryHeader(onStopAll: () -> Unit, isAnyPlaying: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "LIBRARY",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
            )
        }

        if (isAnyPlaying) {
            IconButton(
                onClick = onStopAll,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}
