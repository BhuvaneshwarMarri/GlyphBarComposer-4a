package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.ScreenHeader

@Composable
fun StudioHeader(
    hasEvents: Boolean,
    showSaveSuccess: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onResetProject: () -> Unit
) {
    ScreenHeader(
        title = "MUSIC STUDIO",
        subtitle = "Sync patterns to audio",
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        actions = {
            if (hasEvents) {
                IconButton(
                    onClick = { if (!showSaveSuccess && !isSaving) onSaveClick() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (showSaveSuccess) Color(0xFF00C853) else Color(0x1A00C853)
                        )
                ) {
                    AnimatedContent(
                        targetState = showSaveSuccess,
                        label = "saveIcon"
                    ) { success ->
                        if (success) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF00C853),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            IconButton(
                onClick = onResetProject,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFF5252))
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}
