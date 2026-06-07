package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.components.ScreenHeader
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel

@Composable
fun PatternLabHeader(uiState: PatternLabUiState, viewModel: PatternLabViewModel) {
    ScreenHeader(
        title = "PATTERN LAB",
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        actions = {
            if (uiState.isPlaying) {
                IconButton(
                    onClick = viewModel::togglePreview,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x1AFF5252))
                ) {
                    Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252))
                }
            } else if ((uiState.previewSteps.isNotEmpty() || uiState.selectedPlaylistA != null || uiState.selectedPlaylistB != null)) {
                IconButton(
                    onClick = viewModel::togglePreview,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x1A00C853))
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF00C853))
                }
            }
        }
    )
}
