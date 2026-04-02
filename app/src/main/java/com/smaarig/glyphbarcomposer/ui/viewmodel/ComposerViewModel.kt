package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComposerUiState(
    val glyphIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val durationMs: Float = 1000f,
    val currentSequenceSteps: List<GlyphSequence> = emptyList(),
    val sequenceName: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val activePlaylistId: Long? = null
)

class ComposerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val glyphController = GlyphController.getInstance(application)

    private val _uiState = MutableStateFlow(ComposerUiState())
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    val allPlaylists = playlistDao.getAllPlaylists()

    private var playbackJob: Job? = null

    private val channels = listOf(
        Glyph.Code_25111.A_1,
        Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_6
    )

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

    fun onDurationChange(newDuration: Float) {
        _uiState.update { it.copy(durationMs = newDuration) }
    }

    fun onSequenceNameChange(newName: String) {
        _uiState.update { it.copy(sequenceName = newName) }
    }

    fun addStep() {
        _uiState.update { state ->
            val newSteps = state.currentSequenceSteps + GlyphSequence(getIntensitiesMap(), state.durationMs.toInt())
            state.copy(currentSequenceSteps = newSteps)
        }
    }

    fun clearSequence() {
        if (_uiState.value.isPlaying) return
        _uiState.update { it.copy(currentSequenceSteps = emptyList()) }
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
            glyphIntensities = listOf(0, 0, 0, 0, 0, 0)
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

    fun savePlaylist() {
        val state = _uiState.value
        if (state.sequenceName.isBlank() || state.currentSequenceSteps.isEmpty()) return

        viewModelScope.launch {
            val playlist = Playlist(name = state.sequenceName)
            val playlistId = playlistDao.insertPlaylist(playlist)
            val sequenceSteps = state.currentSequenceSteps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = playlistId,
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            playlistDao.insertSteps(sequenceSteps)
            
            _uiState.update { it.copy(sequenceName = "", currentSequenceSteps = emptyList()) }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_uiState.value.activePlaylistId == playlist.id) {
                stopPlayback()
            }
            playlistDao.deletePlaylist(playlist)
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
