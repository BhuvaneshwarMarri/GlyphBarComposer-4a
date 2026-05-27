package com.smaarig.glyphbarcomposer.ui.screens

import androidx.compose.animation.AnimatedVisibility
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

/**
 * Bottom sheet for adding a new notification hook.
 *
 * Flow:
 *  1. User has already selected an app (passed in via [selectedApp]).
 *  2. If the app is progress-only (Spotify, YouTube), only the progress-sync
 *     option is shown — the channel picker is skipped and progress-sync is pre-selected.
 *  3. Otherwise the user:
 *     a. Picks a notification channel from the app's real channels (or "All notifications")
 *     b. Chooses a glyph sequence playlist
 *     c. Optionally enables progress-sync for apps that support it
 *
 * @param selectedApp       The app the user long-pressed and chose to configure.
 * @param channels          Real notification channels for [selectedApp], from ViewModel.
 * @param isLoadingChannels True while channels are being fetched.
 * @param playlists         All available glyph playlists.
 * @param onConfirm         Called with (channelId?, channelName?, playlistId, isProgressSync).
 * @param onDismiss         Called when the sheet is dismissed without saving.
 */
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

    // State
    var selectedChannelId   by remember { mutableStateOf<String?>(null) }
    var selectedChannelName by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId  by remember { mutableStateOf<Long?>(null) }
    var isProgressSync      by remember { mutableStateOf(isProgressOnly) }

    // Pre-select "All notifications" for non-progress-only apps when channels load
    // And auto-detect progress-sync from channel names
    LaunchedEffect(channels) {
        if (channels.isEmpty() && !isLoadingChannels && !isProgressOnly) {
            selectedChannelId   = null
            selectedChannelName = null
        }
        
        // Auto-detect progress sync based on channel names
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Text(
            text = "Add Hook — ${selectedApp.appName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
        )

        if (isProgressOnly) {
            // Progress-only banner
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${selectedApp.appName} is a media app. Only progress-sync mode is available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Channel Picker (hidden for progress-only apps) ───────────────────
        if (!isProgressOnly) {
            Text(
                "Notification Channel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            when {
                isLoadingChannels -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reading notification channels…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                channels.isEmpty() -> {
                    // No channels found — fall back to category selection
                    Text(
                        "No specific channels found. The hook will trigger on all notifications from this app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selectedChannelId   = null
                    selectedChannelName = null
                }

                else -> {
                    // "All notifications" option
                    ChannelRow(
                        name      = "All Notifications",
                        subtitle  = "Trigger on any notification from this app",
                        selected  = selectedChannelId == null,
                        onClick   = { selectedChannelId = null; selectedChannelName = null }
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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

            Spacer(Modifier.height(16.dp))
        }

        // ── Progress Sync Toggle (always visible, locked for progress-only) ──
        if (!isProgressOnly) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sync Progress Bar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Maps notification progress (0–100%) to glyph steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked  = isProgressSync,
                    onCheckedChange = { isProgressSync = it }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Playlist Picker ──────────────────────────────────────────────────
        Text(
            "Glyph Sequence",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        if (playlists.isEmpty()) {
            Text(
                "No sequences yet. Create one in the Composer tab first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(playlists, key = { it.playlist.id }) { pwSteps ->
                    val pl = pwSteps.playlist
                    PlaylistRow(
                        name     = pl.name,
                        subtitle = "${pwSteps.steps.size} step${if (pwSteps.steps.size != 1) "s" else ""}",
                        selected = selectedPlaylistId == pl.id,
                        onClick  = { 
                            selectedPlaylistId = if (selectedPlaylistId == pl.id) null else pl.id 
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Confirm / Cancel ─────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }

            Button(
                onClick  = {
                    onConfirm(selectedChannelId, selectedChannelName, selectedPlaylistId, isProgressSync)
                },
                enabled  = isProgressSync || selectedPlaylistId != null,
                modifier = Modifier.weight(1f)
            ) { Text("Add Hook") }
        }
    }
}

@Composable
private fun ChannelRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(visible = selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}