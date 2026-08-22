package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.components.ScreenHeader

@Composable
fun LibraryHeader(onStopAll: () -> Unit, isAnyPlaying: Boolean) {
    ScreenHeader(
        title = "LIBRARY",
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        actions = {
            if (isAnyPlaying) {
                IconButton(
                    onClick = onStopAll,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x1AFF5252))
                ) {
                    Icon(
                        Icons.Default.Stop,
                        null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}
