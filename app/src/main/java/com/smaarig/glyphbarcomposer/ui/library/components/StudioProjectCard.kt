package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents

@Composable
fun StudioProjectCard(
    project: MusicProjectWithEvents,
    isActive: Boolean,
    isPlaying: Boolean,
    isAudioMissing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) Color(0xFF00C853) else if (isAudioMissing) Color(0xFFFF5252).copy(alpha = 0.5f) else Color(
                0xFF222222
            )
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFF00C853)
                        else if (isAudioMissing) Color(0xFFFF5252).copy(alpha = 0.1f)
                        else Color(0xFF1A1A1A)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isActive && isPlaying -> Icons.Default.Pause
                        isAudioMissing -> Icons.Default.MusicOff
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = if (isActive) Color.Black else if (isAudioMissing) Color(0xFFFF5252) else Color.Gray
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${project.events.size} sync events",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAudioMissing) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "• MISSING AUDIO",
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(0.4f))
            }
        }
    }
}
