package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Color(0xFFFF1744)    // 6 – RED (Full)
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

fun labelColor(intensity: Int) =
    if (intensity >= 2) Color(0xFF111111) else Color(0xFFFFFFFF)

val statusLabel = listOf("", "LOW", "MED", "HIGH", "ON", "ON", "ON")

@Composable
fun GlyphBox(
    label: String,
    intensity: Int,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit,
    enabled: Boolean = true,
    isRed: Boolean = false
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (intensity > 0) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val fillColor by animateColorAsState(
        targetValue = intensityColor[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "fill"
    )
    val borderColor by animateColorAsState(
        targetValue = intensityBorder[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "border"
    )

    val dragProgress = (kotlin.math.abs(accumulatedDrag).coerceAtMost(30f) / 30f)
    val dragOverlayAlpha = dragProgress * 0.18f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (intensity > 0) borderColor else Color(0xFF222222), RoundedCornerShape(12.dp))
            .background(fillColor)
            .then(if (enabled) {
                Modifier
                    .pointerInput(intensity) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (isRed) return@detectVerticalDragGestures
                                change.consume()
                                accumulatedDrag += dragAmount
                                val threshold = 28f
                                if (accumulatedDrag > threshold) {
                                    if (intensity > 0) onIntensityChange(intensity - 1)
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag < -threshold) {
                                    if (intensity < 3) onIntensityChange(intensity + 1)
                                    accumulatedDrag = 0f
                                }
                            },
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f }
                        )
                    }
                    .clickable {
                        if (isRed) {
                            if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
                        } else {
                            if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
                        }
                    }
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dragOverlayAlpha > 0f && enabled) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (accumulatedDrag < 0) Color.White.copy(alpha = dragOverlayAlpha)
                        else Color.Black.copy(alpha = dragOverlayAlpha)
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor(intensity)
            )
            if (intensity > 0) {
                Text(
                    text = statusLabel[intensity],
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor(intensity).copy(alpha = 0.75f),
                    letterSpacing = 0.8.sp
                )
            }
        }
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
    6 -> com.nothing.ketchum.Glyph.Code_22111.E1
    else -> 0
}

@Composable
fun GlyphPreviewBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glyphController = remember { GlyphController.getInstance(context) }
    val intensities by glyphController.currentIntensities.collectAsState()
    val isBatteryEnabled by glyphController.isBatteryFeatureEnabled.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            glyphController.toggleBatteryFeature(true)
            context.startForegroundService(Intent(context, BatteryService::class.java))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.width(70.dp))

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
fun ModernBottomNavigationBar(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(),
        color = Color(0xFF111111).copy(alpha = 0.85f),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route
                val animatedScale by animateFloatAsState(if (selected) 1.15f else 1f)
                val animatedColor by animateColorAsState(if (selected) Color.White else Color.Gray)

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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                // Minimalist App Logo
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
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route
                val animatedScale by animateFloatAsState(if (selected) 1.25f else 1f)

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
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.White.copy(0.12f)
                    )
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
