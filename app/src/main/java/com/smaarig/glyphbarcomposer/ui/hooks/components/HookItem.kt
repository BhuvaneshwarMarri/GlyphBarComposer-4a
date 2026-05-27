package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HookItem(
    hookWithPlaylist: NotificationHookWithPlaylist,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hook = hookWithPlaylist.hook
    val playlist = hookWithPlaylist.playlist
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Swipe-to-reveal delete
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipe_offset"
    )

    // Test button pulse animation
    var isTesting by remember { mutableStateOf(false) }
    val testScale by animateFloatAsState(
        targetValue = if (isTesting) 0.9f else 1f,
        animationSpec = tween(100),
        label = "test_scale"
    )

    // Debounce toggle to prevent rapid flipping
    var isToggling by remember { mutableStateOf(false) }

    // App icon from PackageManager
    val appIcon = remember(hook.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(hook.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    val deleteThreshold = -160f
    val isRevealed = offsetX < deleteThreshold / 2

    Box(modifier = modifier.fillMaxWidth()) {
        // Background delete action revealed by swipe
        AnimatedVisibility(
            visible = isRevealed,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFB71C1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Main card
        Surface(
            color = Color(0xFF1C1C1C),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX + delta).coerceIn(deleteThreshold, 0f)
                        offsetX = newOffset
                    },
                    onDragStopped = {
                        if (offsetX < deleteThreshold * 0.75f) {
                            onDelete()
                            offsetX = 0f
                        } else {
                            offsetX = 0f
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App icon or fallback
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            androidx.compose.foundation.Image(
                                bitmap = appIcon,
                                contentDescription = hook.appName,
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Text(
                                text = hook.appName.firstOrNull()?.uppercase() ?: "?",
                                color = Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hook.appName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))

                        // Channel badge
                        val channelLabel = when {
                            hook.notificationChannelName != null -> hook.notificationChannelName
                            hook.notificationType == "ALL" -> "All notifications"
                            else -> hook.notificationType.lowercase().replaceFirstChar { it.uppercase() }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (hook.isProgressSync) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = if (hook.isProgressSync) Color(0xFF00BFA5) else Color(0xFF666666),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = channelLabel,
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Playlist name
                        if (playlist != null) {
                            Spacer(Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF555555),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = playlist.name,
                                    color = Color(0xFF666666),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Toggle
                    Switch(
                        checked = hook.isEnabled,
                        onCheckedChange = { newVal ->
                            if (!isToggling) {
                                isToggling = true
                                onToggle(newVal)
                                scope.launch {
                                    delay(300)
                                    isToggling = false
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00BFA5),
                            uncheckedThumbColor = Color(0xFF888888),
                            uncheckedTrackColor = Color(0xFF333333),
                            uncheckedBorderColor = Color(0xFF444444)
                        )
                    )
                }

                // Progress sync badge + Test button row
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tags row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hook.isProgressSync) {
                            HookChip(label = "Progress Sync", color = Color(0xFF00BFA5))
                        }
                        if (!hook.isEnabled) {
                            HookChip(label = "Disabled", color = Color(0xFF666666))
                        }
                        if (hook.notificationChannelId != null) {
                            HookChip(label = "Channel", color = Color(0xFF7B61FF))
                        }
                    }

                    // Test button
                    OutlinedButton(
                        onClick = {
                            if (!isTesting) {
                                isTesting = true
                                onTest()
                                scope.launch {
                                    delay(1500)
                                    isTesting = false
                                }
                            }
                        },
                        enabled = !isTesting && hook.isEnabled,
                        modifier = Modifier
                            .height(30.dp)
                            .scale(testScale),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFD54F),
                            disabledContentColor = Color(0xFF555555)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isTesting && hook.isEnabled) Color(0xFF555533) else Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color(0xFFFFD54F),
                                strokeWidth = 1.5.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Testing…", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Text("Test", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HookChip(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}