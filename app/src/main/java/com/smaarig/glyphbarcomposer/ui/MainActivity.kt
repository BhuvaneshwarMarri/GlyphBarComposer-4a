package com.smaarig.glyphbarcomposer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicSyncViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel
import com.smaarig.glyphbarcomposer.controller.GlyphController
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            intensities.forEach { intensity ->
                val color = when (intensity) {
                    1 -> Color(0xFF686868)
                    2 -> Color(0xFFCDCDCD)
                    3 -> Color(0xFFFFFFFF)
                    else -> Color(0xFF1C1C1C)
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current as ComponentActivity

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
            val viewModel: ComposerViewModel = viewModel(viewModelStoreOwner = activity)
            ComposerScreen(viewModel = viewModel)
        }
        composable(Screen.PatternLab.route) {
            val viewModel: PatternLabViewModel = viewModel(viewModelStoreOwner = activity)
            PatternLabScreen(viewModel = viewModel)
        }
        composable(Screen.MusicSync.route) {
            val viewModel: MusicSyncViewModel = viewModel(viewModelStoreOwner = activity)
            MusicSyncScreen(viewModel = viewModel)
        }
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(viewModelStoreOwner = activity)
            val composerViewModel: ComposerViewModel = viewModel(viewModelStoreOwner = activity)
            val musicSyncViewModel: MusicSyncViewModel = viewModel(viewModelStoreOwner = activity)
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
    // 6 squares animation
    val squareCount = 6
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
            
            // 6 Loading Squares
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                animationStates.forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = alpha.value))
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
