package com.smaarig.glyphbarcomposer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import com.smaarig.glyphbarcomposer.ui.viewmodel.*

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
    object Composer   : Screen("composer",   "Composer", Icons.Default.Piano)
    object PatternLab : Screen("pattern_lab", "Lab",      Icons.Default.Science)
    object MusicSync  : Screen("music_sync", "Music",     Icons.Default.MusicNote)
    object Library    : Screen("library",    "Library",   Icons.Default.LibraryMusic)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0E0E0E),
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHostContainer(navController, Modifier.padding(innerPadding))
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
        startDestination = Screen.Composer.route,
        modifier = modifier
    ) {
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