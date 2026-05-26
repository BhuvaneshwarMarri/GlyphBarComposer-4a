package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist

@Composable
fun HookItem(
    hookWithPlaylist: NotificationHookWithPlaylist,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val hook = hookWithPlaylist.hook
    val playlist = hookWithPlaylist.playlist

    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(hook.appName, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = if (hook.isProgressSync) "Syncs with Progress" else "Plays: ${playlist?.name ?: "Unknown"}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = hook.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.DarkGray
                )
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}
