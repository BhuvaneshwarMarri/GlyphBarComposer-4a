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

import androidx.compose.material.icons.filled.GraphicEq
import com.smaarig.glyphbarcomposer.ui.ScreenHeader

@Composable
fun StudioHeader(
    uiState: MusicStudioUiState, 
    viewModel: MusicStudioViewModel,
    onSaveClick: () -> Unit
) {
    ScreenHeader(
        title = "MUSIC STUDIO",
        icon = Icons.Default.GraphicEq,
        subtitle = "Sync patterns to audio",
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        actions = {
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
    )
}
