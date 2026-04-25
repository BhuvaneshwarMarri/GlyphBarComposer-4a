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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.utils.PreferenceManager

data class ComposerUiState(
    val glyphIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val durationMs: Float = 1000f,
    val currentSequenceSteps: List<GlyphSequence> = emptyList(),
    val sequenceName: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val activePlaylistId: Long? = null,
    val selectedChannelIndex: Int = 0,
    val useOldVersion: Boolean = true
)

class ComposerViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {
    private val glyphController = GlyphController.getInstance(application)
    private val prefManager = PreferenceManager(application)

    private val _uiState = MutableStateFlow(ComposerUiState(useOldVersion = prefManager.useOldVersion))
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    val allPlaylists = repository.allPlaylists

    private var playbackJob: Job? = null

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
        if (_uiState.value.isPlaying) return
        
        _uiState.update { state ->
            val newList = state.glyphIntensities.toMutableList().apply {
                this[index] = newIntensity
            }
            state.copy(glyphIntensities = newList)
        }
        
        glyphController.applyGlyphStateWithIntensities(getIntensitiesMap(), 2000)
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
        _uiState.update { state ->
            val newSteps = state.currentSequenceSteps + GlyphSequence(getIntensitiesMap(), state.durationMs.toInt())
            state.copy(currentSequenceSteps = newSteps)
        }
    }

    fun removeStep(index: Int) {
        if (_uiState.value.isPlaying) return
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
        
        _uiState.update { it.copy(
            glyphIntensities = channels.map { ch -> step.channelIntensities[ch] ?: 0 },
            durationMs = step.durationMs.toFloat()
        ) }
        
        glyphController.applyGlyphStateWithIntensities(step.channelIntensities, 2000)
    }

    fun clearSequence() {
        if (_uiState.value.isPlaying) return
        _uiState.update { it.copy(currentSequenceSteps = emptyList()) }
    }

    fun turnOffAllGlyphs() {
        if (_uiState.value.isPlaying) stopPlayback()
        glyphController.turnOffGlyphs()
        _uiState.update { it.copy(glyphIntensities = listOf(0, 0, 0, 0, 0, 0, 0)) }
    }

    fun togglePause() {
        if (_uiState.value.isPlaying) {
            _uiState.update { it.copy(isPaused = !it.isPaused) }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { it.copy(
            isPlaying = false, 
            isPaused = false, 
            activePlaylistId = null,
            glyphIntensities = listOf(0, 0, 0, 0, 0, 0, 0)
        ) }
        glyphController.turnOffGlyphs()
    }

    fun startPlayback(steps: List<GlyphSequence>, playlistId: Long? = null) {
        if (steps.isEmpty()) return
        stopPlayback()
        
        _uiState.update { it.copy(isPlaying = true, activePlaylistId = playlistId) }
        playbackJob = viewModelScope.launch {
            try {
                while (true) {
                    for (step in steps) {
                        while (_uiState.value.isPaused) {
                            delay(100)
                        }
                        
                        _uiState.update { state ->
                            val newList = channels.map { ch -> step.channelIntensities[ch] ?: 0 }
                            state.copy(glyphIntensities = newList)
                        }
                        
                        glyphController.applyGlyphStateWithIntensities(
                            step.channelIntensities, step.durationMs
                        )
                        delay(step.durationMs.toLong() + 50)
                    }
                }
            } finally {
                // Cleanup handled in stopPlayback
            }
        }
    }

    fun savePlaylist(name: String) {
        val state = _uiState.value
        if (name.isBlank() || state.currentSequenceSteps.isEmpty()) return

        viewModelScope.launch {
            val playlist = Playlist(name = name)
            val playlistSteps = state.currentSequenceSteps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = 0, // Assigned by repository
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            repository.savePlaylist(playlist, playlistSteps)
            
            _uiState.update { it.copy(sequenceName = "", currentSequenceSteps = emptyList()) }
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
