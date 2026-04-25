package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps

@Composable
fun SavedSequenceCard(
    playlist: PlaylistWithSteps,
    isActive: Boolean,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF00C853) else Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    !isActive -> Icons.AutoMirrored.Filled.PlaylistPlay
                    isPlaying && !isPaused -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                }
                Icon(icon, null, tint = if (isActive) Color.Black else Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.playlist.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("${playlist.steps.size} steps", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(0.4f))
            }
        }
    }
}
