package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.BatteryService
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
    6 -> com.nothing.ketchum.Glyph.Code_22111.E1
    else -> 0
}

@Composable
fun GlyphPreviewBar(modifier: Modifier = Modifier, currentRoute: String? = null) {
    val context = LocalContext.current
    val glyphController = remember { GlyphController.getInstance(context) }
    val intensities by glyphController.currentIntensities.collectAsState()
    val isBatteryEnabled by glyphController.isBatteryFeatureEnabled.collectAsState()
    var showHelp by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            glyphController.toggleBatteryFeature(true)
            context.startForegroundService(Intent(context, BatteryService::class.java))
        }
    }

    if (showHelp) {
        HelpDialog(currentRoute = currentRoute) { showHelp = false }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.width(70.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            intensities.forEachIndexed { index, intensity ->
                val isRedGlyph = index == 6
                val finalIntensity = if (isRedGlyph && intensity > 0 && intensity < 4) 6 else intensity
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

@Composable
fun HelpDialog(currentRoute: String?, onDismiss: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    
    val helpSteps = when (currentRoute) {
        "composer" -> listOf(
            HelpStep("GLYPH GRID", "The heart of your creation. Tap any segment to toggle the light for the current frame.", Icons.Default.Grid4x4, Alignment.Center),
            HelpStep("TIMELINE", "Manage your sequence here. Add, clone, or seek through frames using the slider.", Icons.Default.ViewTimeline, Alignment.BottomCenter),
            HelpStep("PREVIEW", "Watch your lights in real-time. The top bar mirrors exactly what's happening on your device.", Icons.Default.Visibility, Alignment.TopCenter)
        )
        "pattern_lab" -> listOf(
            HelpStep("SELECTORS", "Tap BASE A or B to load existing sequences from your collection.", Icons.Default.Collections, Alignment.TopCenter),
            HelpStep("TRANSFORMS", "Fine-tune patterns with speed, inversion, and mirroring controls.", Icons.Default.Transform, Alignment.Center),
            HelpStep("MIX ENGINE", "Crossfade between sequences or use mathematical blend modes for complex visuals.", Icons.Default.SettingsInputComponent, Alignment.BottomCenter)
        )
        "music_studio" -> listOf(
            HelpStep("AUDIO SOURCE", "Load a song or use ambient mode to sync lights with the world around you.", Icons.Default.MusicNote, Alignment.TopCenter),
            HelpStep("ANALYSIS", "Pick an algorithm to auto-map audio frequencies to specific Glyph zones.", Icons.Default.Analytics, Alignment.Center),
            HelpStep("OVERLAY", "Add professional accents by recording manual triggers while the music plays.", Icons.Default.Layers, Alignment.BottomCenter)
        )
        "library" -> listOf(
            HelpStep("ARCHIVE", "Your entire creative history is stored here. Tap any item to load it into the engine.", Icons.Default.Folder, Alignment.Center),
            HelpStep("SHARE & EXPORT", "Long-press to share projects as files or export them for the Nothing OS Glyph composer.", Icons.Default.IosShare, Alignment.TopCenter),
            HelpStep("MASTER CONTROL", "Instantly clear all active light processes with the 'Stop All' button.", Icons.Default.PowerSettingsNew, Alignment.BottomCenter)
        )
        else -> listOf(
            HelpStep("GB4AURA ASSIST", "Welcome. Explore the labs to master your device's Glyph interface.", Icons.Default.Info, Alignment.Center)
        )
    }

    if (stepIndex >= helpSteps.size) {
        onDismiss()
        return
    }

    val currentStep = helpSteps[stepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Spotlight Backdrop ──
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) { }) {
                val spotlightSize = when (currentStep.alignment) {
                    Alignment.TopCenter -> size.height * 0.2f
                    Alignment.Center -> size.height * 0.4f
                    Alignment.BottomCenter -> size.height * 0.25f
                    else -> size.height * 0.3f
                }
                val spotlightCenter = when (currentStep.alignment) {
                    Alignment.TopCenter -> Offset(size.width / 2, size.height * 0.15f)
                    Alignment.Center -> Offset(size.width / 2, size.height * 0.45f)
                    Alignment.BottomCenter -> Offset(size.width / 2, size.height * 0.85f)
                    else -> Offset(size.width / 2, size.height / 2)
                }

                drawRect(color = Color.Black.copy(alpha = 0.85f))
                drawCircle(
                    color = Color.Transparent,
                    radius = spotlightSize,
                    center = spotlightCenter,
                    blendMode = BlendMode.Clear
                )
            }

            // ── Help Card ──
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + slideInVertically { it / 2 }).togetherWith(
                        fadeOut(animationSpec = tween(400)) + slideOutVertically { -it / 2 }
                    )
                },
                modifier = Modifier.align(
                    if (currentStep.alignment == Alignment.TopCenter) Alignment.Center 
                    else if (currentStep.alignment == Alignment.BottomCenter) Alignment.Center
                    else Alignment.BottomCenter
                ).padding(24.dp),
                label = "HelpCardTransition"
            ) { step ->
                Surface(
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        shadowElevation = 20f
                        shape = RoundedCornerShape(28.dp)
                        clip = true
                    },
                    color = Color(0xFF151515).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.1f), Color(0xFFFF1744).copy(0.3f))))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFF1744).copy(0.1f)).border(1.dp, Color(0xFFFF1744).copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = step.icon, contentDescription = null, tint = Color(0xFFFF1744), modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Progress Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            helpSteps.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == stepIndex) 16.dp else 8.dp, 8.dp)
                                        .clip(CircleShape)
                                        .background(if (index == stepIndex) Color(0xFFFF1744) else Color(0xFF333333))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("SKIP GUIDE", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { stepIndex++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (stepIndex == helpSteps.size - 1) "GET STARTED" else "CONTINUE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class HelpStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val alignment: Alignment
)

@Composable
fun ModernBottomNavigationBar(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Pure pill — no inset consumption, no outer padding.
    // The call site (MainActivity bottomBar) owns spacing and insets.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color(0xFF111111).copy(alpha = 0.92f),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
        shadowElevation = 24.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route
                val animatedScale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    label = "navScale"
                )
                val animatedColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFFFF1744) else Color.Gray,
                    label = "navColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        tint = animatedColor,
                        modifier = Modifier.size(24.dp).scale(animatedScale)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = screen.label.uppercase(),
                        color = animatedColor,
                        fontSize = 8.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernNavigationRail(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationRail(
        containerColor = Color(0xFF111111),
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                }
                Spacer(Modifier.height(24.dp))
            }
        },
        // No windowInsetsPadding here – avoids double-consuming insets on rotation
        modifier = Modifier
            .fillMaxHeight()
            .width(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route
                val animatedScale by animateFloatAsState(
                    targetValue = if (selected) 1.25f else 1f,
                    label = "railScale"
                )

                NavigationRailItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            modifier = Modifier.size(24.dp).scale(animatedScale)
                        )
                    },
                    label = null,
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF1744),
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.White.copy(0.12f)
                    )
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}