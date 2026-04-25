package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.composer.components.ComposerHeader
import com.smaarig.glyphbarcomposer.ui.composer.components.v1.ComposerScreenOld
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel

@Composable
fun ComposerPortrait(
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
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComposerHeader(
            uiState = uiState,
            viewModel = viewModel,
            powerScale = powerScale,
            onPowerClick = onPowerClick
        )

        if (uiState.useOldVersion) {
            ComposerScreenOld(uiState, viewModel, redViewModel)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlsColumn(uiState, viewModel, Modifier.weight(1f))
                GlyphsColumn(uiState, viewModel, redViewModel, Modifier.width(88.dp))
                DraggableTimeline(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(0.8f)
                )
            }
        }
    }
}
