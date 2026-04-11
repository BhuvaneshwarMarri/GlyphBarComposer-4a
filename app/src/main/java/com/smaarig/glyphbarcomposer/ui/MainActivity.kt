package com.smaarig.glyphbarcomposer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.ui.viewmodel.*
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.BatteryService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphBarComposerTheme {
                MainApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object SplashScreen : Screen("splash", "Splash", Icons.Default.GraphicEq)
    object Composer : Screen("composer", "Composer", Icons.Default.MusicNote)
    object PatternLab : Screen("pattern_lab", "Patterns", Icons.Default.Pattern)
    object MusicSync : Screen("music_sync", "Music Sync", Icons.Default.GraphicEq)
    object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val isSplashScreen = currentRoute == Screen.SplashScreen.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0E0E0E),
        topBar = {
            if (!isSplashScreen) {
                GlyphPreviewBar()
            }
        },
        bottomBar = { 
            if (!isSplashScreen) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHostContainer(navController, Modifier.padding(innerPadding))
    }
}

@Composable
fun GlyphPreviewBar() {
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
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left spacer to balance the switch on the right
        Box(modifier = Modifier.width(70.dp))

        // Center: Glyph Squares
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

        // Right: Battery Toggle
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
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current as ComponentActivity
    val app = activity.application as GlyphApplication
    val factory = GlyphViewModelFactory(app, app.repository)
    
    val redViewModel: RedGlyphViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = factory
    )

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route,
        modifier = modifier
    ) {
        composable(Screen.SplashScreen.route) {
            SplashScreen(onTimeout = {
                navController.navigate(Screen.Composer.route) {
                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Composer.route) {
            val viewModel: ComposerViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
        }
        composable(Screen.PatternLab.route) {
            val viewModel: PatternLabViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            PatternLabScreen(viewModel = viewModel)
        }
        composable(Screen.MusicSync.route) {
            val viewModel: MusicSyncViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            MusicSyncScreen(viewModel = viewModel, redViewModel = redViewModel)
        }
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            val composerViewModel: ComposerViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            val musicSyncViewModel: MusicSyncViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            LibraryScreen(
                viewModel = viewModel,
                composerViewModel = composerViewModel,
                musicSyncViewModel = musicSyncViewModel
            )
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // 7 squares animation
    val squareCount = 7
    val animationStates = List(squareCount) { index ->
        rememberInfiniteTransition(label = "square_$index").animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, delayMillis = index * 100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha_$index"
        )
    }

    LaunchedEffect(key1 = true) {
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GLYPHBAR COMPOSER",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 7 Loading Squares
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                animationStates.forEachIndexed { index, alpha ->
                    val color = if (index == 6) Color(0xFFFF1744) else Color.White
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color.copy(alpha = alpha.value))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Syncing your experience...",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Composer,
        Screen.PatternLab,
        Screen.MusicSync,
        Screen.Library
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color(0xFF161616),
        contentColor = Color.White
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                label = { Text(screen.label) },
                icon = { Icon(screen.icon, contentDescription = null) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF333333),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
