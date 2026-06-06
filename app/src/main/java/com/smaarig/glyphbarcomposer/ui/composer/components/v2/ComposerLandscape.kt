package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.composer.components.ComposerHeader
import com.smaarig.glyphbarcomposer.ui.composer.components.v1.ComposerScreenOldLandscape
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.PlaybackState

@Composable
fun ComposerLandscape(
    uiState: ComposerUiState,
    playbackState: PlaybackState,
    onToggleVersion: (Boolean) -> Unit,
    onClearSequence: () -> Unit,
    onTurnOffAll: () -> Unit,
    onIntensityChange: (Int, Int) -> Unit,
    onChannelSelect: (Int) -> Unit,
    onDurationChange: (Float) -> Unit,
    onAddStep: () -> Unit,
    onRemoveStep: (Int) -> Unit,
    onReorderSteps: (Int, Int) -> Unit,
    onLoadStep: (Int) -> Unit,
    onStartPlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSavePlaylist: (String) -> Unit,
    onRedToggle: (Boolean) -> Unit,
    onPlaySequence: (com.smaarig.glyphbarcomposer.data.PlaylistWithSteps) -> Unit,
    onDeletePlaylist: (com.smaarig.glyphbarcomposer.data.Playlist) -> Unit,
    onTogglePause: () -> Unit,
    powerScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComposerHeader(
            useOldVersion = uiState.useOldVersion,
            hasSteps = uiState.currentSequenceSteps.isNotEmpty(),
            isPlaying = playbackState.isPlaying,
            onToggleVersion = onToggleVersion,
            onClearSequence = onClearSequence,
            powerScale = powerScale,
            onPowerClick = onTurnOffAll
        )

        if (uiState.useOldVersion) {
            ComposerScreenOldLandscape(
                glyphIntensities = uiState.glyphIntensities,
                currentSequenceSteps = uiState.currentSequenceSteps,
                durationMs = uiState.durationMs,
                isPlaying = playbackState.isPlaying,
                isPaused = playbackState.isPaused,
                selectedChannelIndex = uiState.selectedChannelIndex,
                activePlaylistId = uiState.activePlaylistId,
                activePresetName = uiState.activePresetName,
                onIntensityChange = onIntensityChange,
                onDurationChange = onDurationChange,
                onAddStep = onAddStep,
                onRemoveStep = onRemoveStep,
                onClearSequence = onClearSequence,
                onLoadStep = onLoadStep,
                onStartPlayback = onStartPlayback,
                onStopPlayback = onStopPlayback,
                onTogglePause = onTogglePause,
                onSavePlaylist = onSavePlaylist,
                onPlaySequence = onPlaySequence,
                onDeletePlaylist = onDeletePlaylist,
                onChannelSelect = onChannelSelect,
                onRedToggle = onRedToggle
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Horizontal Glyphs & Horizontal Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LandscapeGlyphsRow(
                        selectedChannelIndex = uiState.selectedChannelIndex,
                        glyphIntensities = uiState.glyphIntensities,
                        playbackIntensities = playbackState.intensities,
                        isPlaying = playbackState.isPlaying,
                        onIntensityChange = onIntensityChange,
                        onChannelSelect = onChannelSelect,
                        modifier = Modifier.weight(1f)
                    )
                    LandscapeControlsRow(
                        durationMs = uiState.durationMs,
                        isPlaying = playbackState.isPlaying,
                        onDurationChange = onDurationChange,
                        onAddStep = onAddStep
                    )
                }

                // Right Panel: Timeline
                DraggableTimeline(
                    steps = uiState.currentSequenceSteps,
                    isPlaying = playbackState.isPlaying,
                    onRemoveStep = onRemoveStep,
                    onReorderSteps = onReorderSteps,
                    onLoadStep = onLoadStep,
                    onStartPlayback = onStartPlayback,
                    onStopPlayback = onStopPlayback,
                    onSave = onSavePlaylist,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}
