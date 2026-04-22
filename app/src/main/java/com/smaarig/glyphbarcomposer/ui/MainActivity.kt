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
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.ui.viewmodel.*
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
    object MusicStudio : Screen("music_studio", "Music Studio", Icons.Default.GraphicEq)
    object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orientation = rememberAppOrientation()

    val isSplashScreen = currentRoute == Screen.SplashScreen.route
    val screens = listOf(
        Screen.Composer,
        Screen.PatternLab,
        Screen.MusicStudio,
        Screen.Library
    )

    if (orientation == AppOrientation.Landscape && !isSplashScreen) {
        // Landscape: fixed TopBar, sidebar NavigationRail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                // Apply horizontal insets (for notches) and bottom insets
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
        ) {
            ModernNavigationRail(navController, screens)

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        color = Color(0xFF0A0A0A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    ) {
                        GlyphPreviewBar()
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHostContainer(navController, Modifier.fillMaxSize())
                }
            }
        }
    } else {
        // Portrait: fixed TopBar, hovering Bottom NavBar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0A0A0A),
            topBar = {
                if (!isSplashScreen) {
                    Surface(
                        color = Color(0xFF0A0A0A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    ) {
                        GlyphPreviewBar()
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content layer — fills the whole screen including behind hovering bars
                NavHostContainer(navController, Modifier.fillMaxSize())

                if (!isSplashScreen) {
                    // Bottom navbar floats over content — pill with shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        ModernBottomNavigationBar(navController, screens)
                    }
                }
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
        composable(Screen.MusicStudio.route) {
            val viewModel: MusicStudioViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            MusicStudioScreen(viewModel = viewModel)
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
            val musicStudioViewModel: MusicStudioViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = factory
            )
            LibraryScreen(
                viewModel = viewModel,
                composerViewModel = composerViewModel,
                musicStudioViewModel = musicStudioViewModel
            )
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
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
