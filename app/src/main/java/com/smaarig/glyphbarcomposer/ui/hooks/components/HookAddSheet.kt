package com.smaarig.glyphbarcomposer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.viewmodel.AppInfo
import com.smaarig.glyphbarcomposer.utils.AppNotificationChannel
import com.smaarig.glyphbarcomposer.utils.AppNotificationChannelHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HookAddSheet(
    selectedApp: AppInfo,
    channels: List<AppNotificationChannel>,
    isLoadingChannels: Boolean,
    playlists: List<PlaylistWithSteps>,
    onConfirm: (
        channelId: String?,
        channelName: String?,
        playlistId: Long?,
        isProgressSync: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val isProgressOnly = selectedApp.isProgressOnly

    var selectedChannelId   by remember { mutableStateOf<String?>(null) }
    var selectedChannelName by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId  by remember { mutableStateOf<Long?>(null) }
    var isProgressSync      by remember { mutableStateOf(isProgressOnly) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "ADD HOOK — ${selectedApp.appName.uppercase()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(24.dp))

        if (isProgressOnly) {
            // Media app flow: Just show info and confirm
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.5f))
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            "Sync Progress Bar",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Text(
                            "is selected for ${selectedApp.appName}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text(
                "Media apps automatically sync their progress to your Glyph lights. No sequence selection needed.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF555555),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            // Regular app flow: Just sequence selection (Channels removed per request)
            SectionLabel("GLYPH SEQUENCE")
            Spacer(Modifier.height(12.dp))

            if (playlists.isEmpty()) {
                Text(
                    "No sequences yet. Create one in the Composer tab first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists, key = { it.playlist.id }) { pwSteps ->
                        val pl = pwSteps.playlist
                        PlaylistRow(
                            name     = pl.name,
                            subtitle = "${pwSteps.steps.size} steps",
                            selected = selectedPlaylistId == pl.id,
                            onClick  = { 
                                selectedPlaylistId = if (selectedPlaylistId == pl.id) null else pl.id 
                                isProgressSync = false // Standard sequences disable progress sync
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) { 
                Text("CANCEL", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp) 
            }

            Button(
                onClick  = {
                    onConfirm(null, null, selectedPlaylistId, isProgressSync)
                },
                enabled  = isProgressSync || selectedPlaylistId != null,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1A1A1A),
                    disabledContentColor = Color(0xFF444444)
                )
            ) { 
                Text("ADD HOOK", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp) 
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF666666),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )
}

@Composable
private fun PlaylistRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) Color(0xFF1A1A1A) else Color(0xFF080808),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF00C853) else Color(0xFF111111))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name, 
                    color = if (selected) Color.White else Color.Gray, 
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    subtitle, 
                    color = if (selected) Color(0xFF00C853).copy(alpha = 0.7f) else Color(0xFF444444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
