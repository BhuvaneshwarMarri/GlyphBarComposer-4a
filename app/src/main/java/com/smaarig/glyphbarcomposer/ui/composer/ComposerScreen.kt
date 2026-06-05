package com.smaarig.glyphbarcomposer.ui.composer

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var isPowerOffAnimating by remember { mutableStateOf(false) }
    val powerScale by animateFloatAsState(
        targetValue = if (isPowerOffAnimating) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { isPowerOffAnimating = false },
        label = "powerOffScale"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ComposerLandscape(
            uiState = uiState,
            playbackState = playbackState,
            onToggleVersion = viewModel::toggleVersion,
            onClearSequence = viewModel::clearSequence,
            onTurnOffAll = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            },
            onIntensityChange = viewModel::onIntensityChange,
            onChannelSelect = viewModel::setSelectedChannel,
            onDurationChange = viewModel::onDurationChange,
            onAddStep = viewModel::addStep,
            onRemoveStep = viewModel::removeStep,
            onReorderSteps = viewModel::reorderSteps,
            onLoadStep = viewModel::loadStep,
            onStartPlayback = { viewModel.startPlayback(uiState.currentSequenceSteps) },
            onStopPlayback = viewModel::stopPlayback,
            onSavePlaylist = viewModel::savePlaylist,
            onRedToggle = { redViewModel.setRed(it) },
            onPlaySequence = viewModel::playSequence,
            onDeletePlaylist = viewModel::deletePlaylist,
            onTogglePause = viewModel::togglePause,
            powerScale = powerScale
        )
    } else {
        ComposerPortrait(
            uiState = uiState,
            playbackState = playbackState,
            onToggleVersion = viewModel::toggleVersion,
            onClearSequence = viewModel::clearSequence,
            onTurnOffAll = {
                isPowerOffAnimating = true
                viewModel.turnOffAllGlyphs()
                redViewModel.setRed(false)
            },
            onIntensityChange = viewModel::onIntensityChange,
            onChannelSelect = viewModel::setSelectedChannel,
            onDurationChange = viewModel::onDurationChange,
            onAddStep = viewModel::addStep,
            onRemoveStep = viewModel::removeStep,
            onReorderSteps = viewModel::reorderSteps,
            onLoadStep = viewModel::loadStep,
            onStartPlayback = { viewModel.startPlayback(uiState.currentSequenceSteps) },
            onStopPlayback = viewModel::stopPlayback,
            onSavePlaylist = viewModel::savePlaylist,
            onRedToggle = { redViewModel.setRed(it) },
            onPlaySequence = viewModel::playSequence,
            onDeletePlaylist = viewModel::deletePlaylist,
            onTogglePause = viewModel::togglePause,
            powerScale = powerScale
        )
    }
}
