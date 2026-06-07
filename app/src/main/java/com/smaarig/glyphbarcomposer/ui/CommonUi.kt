package com.smaarig.glyphbarcomposer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.smaarig.glyphbarcomposer.controller.GlyphConstants
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.BatteryService
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

// ─── Intensity palette ──────────────────────────────────────────────────────
val intensityColor = listOf(
    Color(0xFF1C1C1C),   // 0 – OFF
    Color(0xFF686868),   // 1 – LOW
    Color(0xFFCDCDCD),   // 2 – MED
    Color(0xFFFFFFFF),   // 3 – HIGH
    Color(0xFFC62828),   // 4 – RED (Low)
    Color(0xFFEF5350),   // 5 – RED (Med)
    Color(0xFFFF1744),   // 6 – RED (Full)
)

val intensityBorder = listOf(
    Color(0xFF3A3A3A),   // 0
    Color(0xFF888888),   // 1
    Color(0xFFE0E0E0),   // 2
    Color(0xFFFFFFFF),   // 3
    Color(0xFF5A1010),   // 4 - Red Border
    Color(0xFF8E2A2A),   // 5 - Red Border
    Color(0xFFF44336)    // 6 - Red Border
)

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = nothingFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle.uppercase(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = nothingFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            content = actions
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
}

fun getChannelForIndex(index: Int): Int = when (index) {
    0 -> com.nothing.ketchum.Glyph.Code_25111.A_1
    1 -> com.nothing.ketchum.Glyph.Code_25111.A_2
    2 -> com.nothing.ketchum.Glyph.Code_25111.A_3
    3 -> com.nothing.ketchum.Glyph.Code_25111.A_4
    4 -> com.nothing.ketchum.Glyph.Code_25111.A_5
    5 -> com.nothing.ketchum.Glyph.Code_25111.A_6
    6 -> GlyphConstants.PHONE_4A_CHANNELS[6]
    else -> 0
}

val glyphChannels = GlyphConstants.PHONE_4A_CHANNELS

@Composable
fun YouTubePlayer(
    videoId: String,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(this)

                val options = IFramePlayerOptions.Builder(context)
                    .controls(1)
                    .fullscreen(0)
                    .build()

                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                }, options)
            }
        },
        onRelease = { it.release() },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
fun GlyphPreviewBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val glyphController = remember { GlyphController.getInstance(context) }
    val intensities by glyphController.currentIntensities.collectAsState()
    val isBatteryEnabled by glyphController.isBatteryFeatureEnabled.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            glyphController.toggleBatteryFeature(true)
            context.startForegroundService(Intent(context, BatteryService::class.java))
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "HELP & TUTORIAL",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = nothingFont,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "To better understand how to use the GlyphBar Composer, we recommend watching this tutorial video:",
                        color = Color.Gray,
                        fontFamily = nothingFont,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    YouTubePlayer(
                        videoId = "dQw4w9WgXcQ",
                        lifecycleOwner = lifecycleOwner
                    )

                    Spacer(Modifier.height(16.dp))

                    val videoUrl = "https://youtu.be/dQw4w9WgXcQ?si=3mdmf20EWUm6b7bS"
                    Text(
                        text = "Watch on YouTube",
                        color = Color(0xFF0086EA),
                        fontFamily = nothingFont,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, videoUrl.toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(
                        "GOT IT",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = nothingFont,
                        letterSpacing = 1.sp
                    )
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(70.dp)
        ) {
            IconButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            intensities.forEachIndexed { index, intensity ->
                val isRedGlyph = index == 6
                val finalIntensity =
                    if (isRedGlyph && intensity > 0 && intensity < 4) 6 else intensity
                val color = intensityColor.getOrElse(finalIntensity) { Color(0xFF1C1C1C) }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(70.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = "Battery Sync",
                tint = if (isBatteryEnabled) Color(0xFF00C853) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Switch(
                checked = isBatteryEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@Switch
                        }
                        glyphController.toggleBatteryFeature(true)
                        context.startForegroundService(Intent(context, BatteryService::class.java))
                    } else {
                        glyphController.toggleBatteryFeature(false)
                        context.stopService(Intent(context, BatteryService::class.java))
                    }
                },
                modifier = Modifier.scale(0.6f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00C853),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }
    }
}

// ─── Nothing-style Bottom Navigation Bar ────────────────────────────────────
// Selected tab: full-height white pill that visually "hovers" over the dark
// bar via shadow elevation. Content inside the pill inverts to near-black.
// Unselected tabs: icon + label in dim gray, no background.
@Composable
fun ModernBottomNavigationBar(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, Color(0xFF242424)),
        shadowElevation = 24.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Tight uniform padding so the pill can fill most of the bar height
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route

                // Pill background: gray when selected, transparent otherwise
                val pillBg by animateColorAsState(
                    targetValue = if (selected) Color(0xFFE0E0E0) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "pillBg"
                )
                // Content color inverts inside the gray pill for contrast
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF111111) else Color(0xFF666666),
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "contentColor"
                )
                // Animate elevation so the pill smoothly "lifts" into view
                val pillElevation by animateDpAsState(
                    targetValue = if (selected) 10.dp else 0.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "pillElevation"
                )

                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        // Draw shadow outside the clip so it bleeds onto the dark bar
                        .shadow(
                            elevation = pillElevation,
                            shape = RoundedCornerShape(26.dp),
                            clip = false,
                            ambientColor = Color.White.copy(alpha = 0.08f),
                            spotColor = Color.White.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = screen.label.uppercase(),
                            color = contentColor,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = nothingFont
                        )
                    }
                }
            }
        }
    }
}

// ─── Nothing-style Navigation Rail ──────────────────────────────────────────
// Same pill treatment as the bottom bar but oriented vertically.
@Composable
fun ModernNavigationRail(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationRail(
        containerColor = Color.Black,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        },
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route

                val pillBg by animateColorAsState(
                    targetValue = if (selected) Color(0xFFE0E0E0) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "railPillBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF111111) else Color(0xFF666666),
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "railContentColor"
                )
                val pillElevation by animateDpAsState(
                    targetValue = if (selected) 10.dp else 0.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "railPillElevation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(vertical = 2.dp)
                        .shadow(
                            elevation = pillElevation,
                            shape = RoundedCornerShape(27.dp),
                            clip = false,
                            ambientColor = Color.White.copy(alpha = 0.08f),
                            spotColor = Color.White.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(27.dp))
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = screen.label.uppercase(),
                            color = contentColor,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = nothingFont
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
