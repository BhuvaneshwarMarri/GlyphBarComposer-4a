package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel

@Composable
fun PatternLabHeader(uiState: PatternLabUiState, viewModel: PatternLabViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "PATTERN LAB",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
        )
        
        if (uiState.isPlaying) {
            IconButton(
                onClick = viewModel::togglePreview,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252))
            }
        } else if ((uiState.previewSteps.isNotEmpty() || uiState.selectedPlaylistA != null || uiState.selectedPlaylistB != null)) {
            IconButton(
                onClick = viewModel::togglePreview,
                modifier = Modifier.clip(CircleShape).background(Color(0x1A00C853))
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF00C853))
            }
        }
    }
}
