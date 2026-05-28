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

    LaunchedEffect(channels) {
        if (channels.isEmpty() && !isLoadingChannels && !isProgressOnly) {
            selectedChannelId   = null
            selectedChannelName = null
        }
        
        val progressKeywords = listOf("progress", "download", "upload", "sync", "transfer", "media", "playback")
        val hasProgressChannel = channels.any { ch ->
            val name = ch.name?.lowercase() ?: ""
            progressKeywords.any { it in name }
        }
        if (hasProgressChannel && !isProgressSync) {
            isProgressSync = true
        }
    }

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

        if (isProgressOnly) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF2A2A2A))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00BFA5),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${selectedApp.appName} is a media app. Only progress-sync mode is available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!isProgressOnly) {
            SectionLabel("NOTIFICATION CHANNEL")
            Spacer(Modifier.height(12.dp))

            when {
                isLoadingChannels -> {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Reading channels…", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                channels.isEmpty() -> {
                    Text(
                        "No specific channels found. Triggers on all notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    selectedChannelId   = null
                    selectedChannelName = null
                }

                else -> {
                    ChannelRow(
                        name      = "All Notifications",
                        subtitle  = "Trigger on any notification",
                        selected  = selectedChannelId == null,
                        onClick   = { selectedChannelId = null; selectedChannelName = null }
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(channels, key = { it.id }) { ch ->
                            ChannelRow(
                                name     = AppNotificationChannelHelper.formatChannelName(ch),
                                subtitle = buildString {
                                    append(AppNotificationChannelHelper.importanceLabel(ch.importance))
                                    if (!ch.description.isNullOrBlank()) append(" · ${ch.description}")
                                },
                                selected = selectedChannelId == ch.id,
                                onClick  = { selectedChannelId = ch.id; selectedChannelName = ch.name }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (!isProgressOnly) {
            Surface(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isProgressSync) Color(0xFF00BFA5) else Color(0xFF222222))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sync Progress Bar", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            "Maps progress (0–100%) to glyph steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked  = isProgressSync,
                        onCheckedChange = { isProgressSync = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00BFA5),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1A1A1A),
                            uncheckedBorderColor = Color(0xFF333333)
                        )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

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
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.playlist.id }) { pwSteps ->
                    val pl = pwSteps.playlist
                    PlaylistRow(
                        name     = pl.name,
                        subtitle = "${pwSteps.steps.size} steps",
                        selected = selectedPlaylistId == pl.id,
                        onClick  = { 
                            selectedPlaylistId = if (selectedPlaylistId == pl.id) null else pl.id 
                        }
                    )
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
                    onConfirm(selectedChannelId, selectedChannelName, selectedPlaylistId, isProgressSync)
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
private fun ChannelRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) Color(0xFF1A1A1A) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF444444) else Color(0xFF111111))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (selected) Color.Black else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    name, 
                    color = Color.White, 
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle, 
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                    color = Color(0xFF444444),
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
