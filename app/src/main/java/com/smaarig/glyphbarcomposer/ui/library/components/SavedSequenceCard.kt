package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun SavedSequenceCard(
    playlist: PlaylistWithSteps,
    isActive: Boolean,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShareGlyph: () -> Unit,
    onShareCsv: () -> Unit,
    onShareJson: () -> Unit
) {
    var showExportMenu by remember { mutableStateOf(false) }

    val isActuallyPlaying = isActive && isPlaying && !isPaused
    val accentColor = if (isActuallyPlaying) Color(0xFF00E676) else if (isActive) Color(0xFF0086EA) else Color(0xFF222222)
    val cardBg by animateColorAsState(if (isActive) Color(0xFF1A1A1A) else Color(0xFF111111), label = "cardBg")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onPlay() },
        color = cardBg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    !isActive -> Icons.AutoMirrored.Filled.PlaylistPlay
                    isActuallyPlaying -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                }
                Icon(icon, null, tint = if (isActive) accentColor else Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.playlist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = nothingFont
                )
                Text(
                    "${playlist.steps.size} steps".uppercase(),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = nothingFont,
                    letterSpacing = 1.sp
                )
            }
            
            Box {
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as Glyph", color = Color.White, fontFamily = nothingFont, fontSize = 12.sp) },
                        onClick = {
                            showExportMenu = false
                            onShareGlyph()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as CSV", color = Color.White, fontFamily = nothingFont, fontSize = 12.sp) },
                        onClick = {
                            showExportMenu = false
                            onShareCsv()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as JSON", color = Color.White, fontFamily = nothingFont, fontSize = 12.sp) },
                        onClick = {
                            showExportMenu = false
                            onShareJson()
                        }
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
