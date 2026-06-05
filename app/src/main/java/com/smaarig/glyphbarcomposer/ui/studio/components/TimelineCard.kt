package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState

@Composable
fun TimelineCard(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    onSelectEvent: (com.smaarig.glyphbarcomposer.data.MusicStudioEvent?) -> Unit,
    onMoveEvent: (com.smaarig.glyphbarcomposer.data.MusicStudioEvent, Long) -> Unit,
    onResizeEvent: (com.smaarig.glyphbarcomposer.data.MusicStudioEvent, Long, Int) -> Unit,
    onDeleteEvent: (com.smaarig.glyphbarcomposer.data.MusicStudioEvent) -> Unit,
    onSeekMusic: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "TIMELINE",
            color = Color(0xFF555555),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        StudioTimelineEditor(
            uiState = uiState,
            audioPositionMs = audioPositionMs,
            onSelectEvent = onSelectEvent,
            onMoveEvent = onMoveEvent,
            onResizeEvent = onResizeEvent,
            onDeleteEvent = onDeleteEvent,
            onSeekMusic = onSeekMusic,
            modifier = Modifier.height(280.dp)
        )
    }
}
