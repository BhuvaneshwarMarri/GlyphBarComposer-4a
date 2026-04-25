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
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

@Composable
fun TimelineCard(uiState: MusicStudioUiState, audioPositionMs: Int, viewModel: MusicStudioViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TIMELINE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        StudioTimelineEditor(
            uiState = uiState,
            audioPositionMs = audioPositionMs,
            onSelectEvent = viewModel::selectEvent,
            onMoveEvent = viewModel::updateEventPosition,
            onResizeEvent = viewModel::updateEventStartAndDuration,
            onDeleteEvent = viewModel::deleteMusicEvent,
            onSeekMusic = viewModel::seekMusic,
            modifier = Modifier.height(280.dp)
        )
    }
}
