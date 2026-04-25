package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

@Composable
fun StudioHeader(
    uiState: MusicStudioUiState, 
    viewModel: MusicStudioViewModel,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "MUSIC STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
            )
            Text(
                "Sync patterns to audio",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.musicEvents.isNotEmpty()) {
                IconButton(
                    onClick = { if (!uiState.showSaveSuccess && !uiState.isSaving) onSaveClick() },
                    modifier = Modifier.clip(CircleShape).background(
                        if (uiState.showSaveSuccess) Color(0xFF00C853) else Color(0x1A00C853)
                    )
                ) {
                    AnimatedContent(
                        targetState = uiState.showSaveSuccess,
                        label = "saveIcon"
                    ) { success ->
                        if (success) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00C853), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            IconButton(
                onClick = viewModel::resetProject,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}
