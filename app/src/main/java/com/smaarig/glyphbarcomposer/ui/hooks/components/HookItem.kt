package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFB71C1C).copy(alpha = 0.8f)),
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
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (hook.isEnabled) Color(0xFF00C853) else Color(0xFF222222)),
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
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular App Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (hook.isEnabled) Color(0xFF1A1A1A) else Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = appIcon,
                            contentDescription = hook.appName,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                    } else {
                        Text(
                            text = hook.appName.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hook.appName,
                        color = if (hook.isEnabled) Color.White else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val subtitle = when {
                        hook.notificationChannelName != null -> hook.notificationChannelName
                        playlist != null -> playlist.name
                        else -> "Notification Hook"
                    }
                    
                    Text(
                        text = subtitle,
                        color = if (hook.isEnabled) Color.Gray else Color(0xFF444444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actions
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Test button
                    IconButton(
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
                        modifier = Modifier.size(36.dp).scale(testScale)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFFFD54F),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Test",
                                tint = if (hook.isEnabled) Color(0xFFFFD54F).copy(alpha = 0.7f) else Color(0xFF333333),
                                modifier = Modifier.size(20.dp)
                            )
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
                            uncheckedThumbColor = Color(0xFF555555),
                            uncheckedTrackColor = Color(0xFF1A1A1A),
                            uncheckedBorderColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
        }
    }
}
