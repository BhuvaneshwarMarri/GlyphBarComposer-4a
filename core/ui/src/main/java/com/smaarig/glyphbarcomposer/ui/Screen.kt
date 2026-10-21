package com.smaarig.glyphbarcomposer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object SplashScreen : Screen("splash", "Splash", Icons.Default.GraphicEq)
    object Composer : Screen("composer", "Composer", Icons.Default.MusicNote)
    object PatternLab : Screen("pattern_lab", "Patterns", Icons.Default.Pattern)
    object MusicStudio : Screen("music_studio", "Music Studio", Icons.Default.GraphicEq)
    object Hooks : Screen("hooks", "Hooks", Icons.Default.NotificationsActive)
    object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
}
