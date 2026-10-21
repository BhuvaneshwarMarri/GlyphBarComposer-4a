package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

enum class AppOrientation {
    Portrait, Landscape
}

@Composable
fun rememberAppOrientation(): AppOrientation {
    val configuration = LocalConfiguration.current
    return remember(configuration.orientation) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            AppOrientation.Landscape
        } else {
            AppOrientation.Portrait
        }
    }
}
