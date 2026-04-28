package com.smaarig.glyphbarcomposer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.ui.composer.ComposerScreen
import com.smaarig.glyphbarcomposer.ui.library.LibraryScreen
import com.smaarig.glyphbarcomposer.ui.patternlab.PatternLabScreen
import com.smaarig.glyphbarcomposer.ui.studio.MusicStudioScreen
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the activity's intent so the LaunchedEffect in MainApp picks it up
        setIntent(intent)
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
    val context = LocalContext.current
    val activity = LocalActivity.current as ComponentActivity
    val app = activity.application as GlyphApplication
    val factory = GlyphViewModelFactory(app, app.repository)
    val libraryViewModel: LibraryViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = factory
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orientation = rememberAppOrientation()

    // ── Incoming file intent handler ─────────────────────────────────────────
    // Triggered on cold start and whenever onNewIntent fires (setIntent updates activity.intent).
    // Handles both ACTION_VIEW (open file) and ACTION_SEND (share sheet).
    LaunchedEffect(activity.intent) {
        val intent = activity.intent ?: return@LaunchedEffect

        val uri: android.net.Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data

            Intent.ACTION_SEND -> {
                // URI is carried in EXTRA_STREAM for file shares
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            else -> null
        }

        if (uri != null) {
            // Try to take persistable permission (only granted by some providers).
            // Silently ignore if not offered — transient access is enough for import.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }

            libraryViewModel.importItem(context, uri)

            // Clear the intent so recompositions don't re-trigger the import
            activity.intent = Intent()

            // Navigate to Library so the user sees the imported item
            navController.navigate(Screen.Library.route) {
                // Don't pop splash if it hasn't finished yet — just avoid stacking Library twice
                launchSingleTop = true
            }
        }
    }
    // ── End intent handler ───────────────────────────────────────────────────

    val isSplashScreen = currentRoute == Screen.SplashScreen.route
    val screens = listOf(
        Screen.Composer,
        Screen.PatternLab,
        Screen.MusicStudio,
        Screen.Library
    )

    if (orientation == AppOrientation.Landscape && !isSplashScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
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
                NavHostContainer(navController, Modifier.fillMaxSize())

                if (!isSplashScreen) {
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

    val screens = listOf(
        Screen.Composer,
        Screen.PatternLab,
        Screen.MusicStudio,
        Screen.Library
    )

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route,
        modifier = modifier,
        enterTransition = {
            val initialState = initialState.destination.route
            val targetState = targetState.destination.route
            
            val initialIdx = screens.indexOfFirst { it.route == initialState }
            val targetIdx = screens.indexOfFirst { it.route == targetState }

            if (initialIdx != -1 && targetIdx != -1) {
                if (targetIdx > initialIdx) {
                    // Slide to left (moving forward in list)
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(500))
                } else {
                    // Slide to right (moving backward in list)
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(500))
                }
            } else {
                fadeIn(animationSpec = tween(500))
            }
        },
        exitTransition = {
            val initialState = initialState.destination.route
            val targetState = targetState.destination.route
            
            val initialIdx = screens.indexOfFirst { it.route == initialState }
            val targetIdx = screens.indexOfFirst { it.route == targetState }

            if (initialIdx != -1 && targetIdx != -1) {
                if (targetIdx > initialIdx) {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(500))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(500))
                }
            } else {
                fadeOut(animationSpec = tween(500))
            }
        }
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
                text = "GB4Aura",
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