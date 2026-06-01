package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.utils.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComposerUiState(
    val glyphIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val durationMs: Float = 1000f,
    val currentSequenceSteps: List<GlyphSequence> = emptyList(),
    val sequenceName: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val activePlaylistId: Long? = null,
    val editingPlaylistId: Long? = null,
    val selectedChannelIndex: Int = 0,
    val useOldVersion: Boolean = true
)

class ComposerViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {
    private val glyphController = GlyphController.getInstance(application)
    private val prefManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(ComposerUiState(useOldVersion = prefManager.useOldVersion))
    val uiState: StateFlow<ComposerUiState> = combine(
        _uiState,
        glyphController.currentIntensities,
        glyphController.isPlaying,
        glyphController.isPaused
    ) { state, controllerIntensities, playing, paused ->
        state.copy(
            // Use controller intensities only when playing to avoid overwriting user edits
            glyphIntensities = if (playing) controllerIntensities else state.glyphIntensities,
            isPlaying = playing,
            isPaused = paused
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ComposerUiState(useOldVersion = prefManager.useOldVersion)
    )

    val allPlaylists = repository.allPlaylists

    private var loadStepJob: Job? = null
    private var intensityUpdateJob: Job? = null

    private val channels = listOf(
        Glyph.Code_25111.A_1,
        Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_6,
        Glyph.Code_22111.E1
    )

    fun toggleVersion(isOld: Boolean) {
        prefManager.useOldVersion = isOld
        _uiState.update { it.copy(useOldVersion = isOld) }
    }

    fun onIntensityChange(index: Int, newIntensity: Int) {
        if (glyphController.isPlaying.value) return

        // Update local state immediately for snappy UI
        val newList = _uiState.value.glyphIntensities.toMutableList().apply {
            this[index] = newIntensity
        }
        _uiState.update { it.copy(glyphIntensities = newList) }

        intensityUpdateJob?.cancel()
        intensityUpdateJob = viewModelScope.launch {
            delay(20) // Debounce physical Glyph update
            val intensityMap = getIntensitiesMap()
            glyphController.applyGlyphStateWithIntensities(intensityMap, 2000)
        }
    }

    fun setSelectedChannel(index: Int) {
        _uiState.update { it.copy(selectedChannelIndex = index) }
    }

    fun reorderSteps(from: Int, to: Int) {
        val list = _uiState.value.currentSequenceSteps.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _uiState.update { it.copy(currentSequenceSteps = list) }
        }
    }

    fun onDurationChange(newDuration: Float) {
        _uiState.update { it.copy(durationMs = newDuration) }
    }

    fun addStep() {
        val state = _uiState.value
        val intensities = getIntensitiesMap()

        val newSteps =
            state.currentSequenceSteps + GlyphSequence(intensities, state.durationMs.toInt())
        _uiState.update { it.copy(currentSequenceSteps = newSteps) }

        // Debug Log
        android.util.Log.d("ComposerViewModel", "Step added. Total steps: ${newSteps.size}")
    }

    fun removeStep(index: Int) {
        if (glyphController.isPlaying.value) return
        _uiState.update { state ->
            val mutableSteps = state.currentSequenceSteps.toMutableList()
            if (index in mutableSteps.indices) {
                mutableSteps.removeAt(index)
            }
            state.copy(currentSequenceSteps = mutableSteps)
        }
    }

    fun loadStep(index: Int) {
        val state = _uiState.value
        val step = state.currentSequenceSteps.getOrNull(index) ?: return

        _uiState.update {
            it.copy(
                glyphIntensities = channels.map { ch -> step.channelIntensities[ch] ?: 0 },
                durationMs = step.durationMs.toFloat()
            )
        }

        loadStepJob?.cancel()
        loadStepJob = viewModelScope.launch {
            delay(10) // Debounce hardware sync for scrolling
            glyphController.applyGlyphStateWithIntensities(step.channelIntensities, 2000)
        }
    }

    fun clearSequence() {
        if (glyphController.isPlaying.value) return
        _uiState.update { it.copy(currentSequenceSteps = emptyList()) }
    }

    fun turnOffAllGlyphs() {
        glyphController.turnOffGlyphs()
        _uiState.update {
            it.copy(
                glyphIntensities = listOf(0, 0, 0, 0, 0, 0, 0),
                activePlaylistId = null // Ensure everything is reset
            )
        }
    }

    fun togglePause() {
        glyphController.togglePausePlayback()
    }

    fun stopPlayback() {
        glyphController.stopPlayback()
        _uiState.update { it.copy(activePlaylistId = null) }
    }

    fun startPlayback(steps: List<GlyphSequence>, playlistId: Long? = null) {
        if (steps.isEmpty()) return
        glyphController.playSequence(steps, loop = true)
        _uiState.update { it.copy(activePlaylistId = playlistId) }
    }

    fun savePlaylist(name: String) {
        val state = _uiState.value
        if (name.isBlank() || state.currentSequenceSteps.isEmpty()) return

        viewModelScope.launch {
            val playlist = Playlist(
                id = state.editingPlaylistId ?: 0,
                name = name
            )
            val playlistSteps = state.currentSequenceSteps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = playlist.id,
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            repository.savePlaylist(playlist, playlistSteps)

            _uiState.update {
                it.copy(
                    sequenceName = "",
                    currentSequenceSteps = emptyList(),
                    editingPlaylistId = null
                )
            }
        }
    }

    fun editPlaylist(playlist: PlaylistWithSteps) {
        stopPlayback()
        val steps = playlist.steps.sortedBy { it.stepIndex }.map {
            GlyphSequence(it.channelIntensities, it.durationMs)
        }
        _uiState.update {
            it.copy(
                currentSequenceSteps = steps,
                sequenceName = playlist.playlist.name,
                editingPlaylistId = playlist.playlist.id
            )
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_uiState.value.activePlaylistId == playlist.id) {
                stopPlayback()
            }
            repository.deletePlaylist(playlist)
        }
    }

    /**
     * Toggles playback for a saved sequence from the library.
     */
    fun playSequence(playlist: PlaylistWithSteps) {
        val state = _uiState.value
        if (state.activePlaylistId == playlist.playlist.id) {
            togglePause()
        } else {
            val steps = playlist.steps.sortedBy { it.stepIndex }.map {
                GlyphSequence(it.channelIntensities, it.durationMs)
            }
            startPlayback(steps, playlist.playlist.id)
        }
    }

    private fun getIntensitiesMap(): Map<Int, Int> =
        channels.mapIndexed { index, ch -> ch to _uiState.value.glyphIntensities[index] }.toMap()

    override fun onCleared() {
        super.onCleared()
        glyphController.turnOffGlyphs()
    }
}
