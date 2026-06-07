package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.SectionLabel
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

@Composable
fun SequencesTab(
    isPlaying: Boolean,
    isPaused: Boolean,
    activeId: Long?,
    activePresetName: String?,
    playlists: List<PlaylistWithSteps>,
    viewModel: ComposerViewModel,
    onEdit: (PlaylistWithSteps) -> Unit,
    onShareGlyph: (PlaylistWithSteps) -> Unit,
    onShareCsv: (PlaylistWithSteps) -> Unit,
    onShareJson: (PlaylistWithSteps) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionLabel("GLYPH  PRESETS") }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetSequences.forEach { preset ->
                    val isActive = activePresetName == preset.name
                    PresetCard(preset, isActive) {
                        if (isActive && isPlaying) {
                            viewModel.stopPlayback()
                        } else {
                            viewModel.startPlayback(preset.steps, null, preset.name)
                        }
                    }
                }
            }
        }

        item { SectionLabel("MY SEQUENCES") }
        if (playlists.isEmpty()) {
            item {
                EmptyStateView(
                    Icons.Default.Create,
                    "No Sequences",
                    "Create one in the Composer tab"
                )
            }
        } else {
            items(playlists, key = { it.playlist.id }) { playlist ->
                SavedSequenceCard(
                    playlist = playlist,
                    isActive = activeId == playlist.playlist.id,
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    onPlay = { viewModel.playSequence(playlist) },
                    onDelete = { viewModel.deletePlaylist(playlist.playlist) },
                    onEdit = { onEdit(playlist) },
                    onShareGlyph = { onShareGlyph(playlist) },
                    onShareCsv = { onShareCsv(playlist) },
                    onShareJson = { onShareJson(playlist) }
                )
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}
