package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState

@Composable
fun StudioPlayerCard(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onChangeAudio: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rotating Vinyl
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ), label = "rotation"
                )
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { if (uiState.isAudioPlaying) rotationZ = rotation }
                        .clip(CircleShape)
                        .background(Color(0xFF080808))
                        .border(1.5.dp, Color(0xFF333333), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(40.dp)) {
                        val r = size.minDimension / 2
                        drawCircle(Color(0xFF1A1A1A), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        drawCircle(Color(0xFF1A1A1A), radius = r * 0.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint = if (uiState.isAudioPlaying) Color(0xFF00C853) else Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        uiState.audioName ?: "Select a Track",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatTime(audioPositionMs)} / ${formatTime(uiState.audioDurationMs)}",
                        color = Color.Gray, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = onChangeAudio,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Slider(
                    value = audioPositionMs.toFloat(),
                    onValueChange = onSeek,
                    valueRange = 0f..uiState.audioDurationMs.toFloat().coerceAtLeast(1f),
                    enabled = uiState.isAnalysisComplete,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF222222)
                    ),
                    modifier = Modifier.weight(1f).height(24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isAnalysisComplete) Color.White else Color(0xFF1A1A1A))
                        .clickable(enabled = uiState.isAnalysisComplete) { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = if (uiState.isAnalysisComplete) Color.Black else Color(0xFF333333),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
