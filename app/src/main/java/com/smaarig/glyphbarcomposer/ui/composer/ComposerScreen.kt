package com.smaarig.glyphbarcomposer.ui.composer

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.ui.composer.components.v2.ComposerLandscape
import com.smaarig.glyphbarcomposer.ui.composer.components.v2.ComposerPortrait
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerScreen(
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isPowerOffAnimating by remember { mutableStateOf(false) }
    val powerScale by animateFloatAsState(
        targetValue = if (isPowerOffAnimating) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { isPowerOffAnimating = false },
        label = "powerOffScale"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ComposerLandscape(
            uiState = uiState,
            viewModel = viewModel,
            redViewModel = redViewModel,
            powerScale = powerScale,
            onPowerClick = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            }
        )
    } else {
        ComposerPortrait(
            uiState = uiState,
            viewModel = viewModel,
            redViewModel = redViewModel,
            powerScale = powerScale,
            onPowerClick = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            }
        )
    }
}
