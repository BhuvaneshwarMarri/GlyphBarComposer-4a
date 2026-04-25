package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.composer.components.ComposerHeader
import com.smaarig.glyphbarcomposer.ui.composer.components.v1.ComposerScreenOldLandscape
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerLandscape(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    redViewModel: RedGlyphViewModel,
    powerScale: Float,
    onPowerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComposerHeader(
            uiState = uiState,
            viewModel = viewModel,
            powerScale = powerScale,
            onPowerClick = onPowerClick
        )

        if (uiState.useOldVersion) {
            ComposerScreenOldLandscape(uiState, viewModel, redViewModel)
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Horizontal Glyphs & Horizontal Controls
                Column(
                    modifier = Modifier.weight(1.3f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LandscapeGlyphsRow(uiState, viewModel, redViewModel, Modifier.weight(1f))
                    LandscapeControlsRow(uiState, viewModel)
                }

                // Right Panel: Timeline
                DraggableTimeline(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}
