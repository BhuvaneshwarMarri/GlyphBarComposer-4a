package com.smaarig.glyphbarcomposer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF0086EA),
    tertiary = Color(0xFFFFC1CC),
    background = Color.Black,
    surface = Color(0xFF111111),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF0086EA),
    tertiary = Color(0xFFFFC1CC),
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun GlyphBarComposerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Disable the "sparkle" ripple introduced in Android 12+ 
    // by providing a custom RippleConfiguration.
    val rippleConfiguration = RippleConfiguration(
        color = if (darkTheme) Color.White else Color.Black,
        rippleAlpha = androidx.compose.material3.RippleDefaults.RippleAlpha
    )

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
