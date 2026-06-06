package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphConstants
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService
import com.smaarig.glyphbarcomposer.utils.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class ComposerUiState(
    val glyphIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val durationMs: Float = 1000f,
    val currentSequenceSteps: List<GlyphSequence> = emptyList(),
    val sequenceName: String = "",
    val activePlaylistId: Long? = null,
    val activePresetName: String? = null,
    val editingPlaylistId: Long? = null,
    val selectedChannelIndex: Int = 0,
    val useOldVersion: Boolean = true
)

@Stable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val intensities: List<Int> = List(7) { 0 }
)

class ComposerViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {
    private val glyphController = GlyphController.getInstance(application)
    private val prefManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(ComposerUiState(useOldVersion = prefManager.useOldVersion))
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = combine(
        glyphController.isPlaying,
        glyphController.isPaused,
        glyphController.currentIntensities
    ) { playing, paused, intensities ->
        PlaybackState(playing, paused, intensities)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(500),
        PlaybackState()
    )

    val allPlaylists = repository.allPlaylists

    private var loadStepJob: Job? = null
    private var intensityUpdateJob: Job? = null

    private val channels = GlyphConstants.PHONE_4A_CHANNELS

    fun toggleVersion(isOld: Boolean) {
        prefManager.useOldVersion = isOld
        _uiState.update { it.copy(useOldVersion = isOld) }
    }

    fun onIntensityChange(index: Int, newIntensity: Int) {
        if (glyphController.isPlaying.value) return

        // Ensure we have control before applying manual changes
        if (glyphController.activeOwner.value != GlyphController.GlyphOwner.COMPOSER) {
            glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER)
        }

        // Update local state immediately for snappy UI
        val newList = _uiState.value.glyphIntensities.toMutableList().apply {
            this[index] = newIntensity
        }
        _uiState.update { it.copy(glyphIntensities = newList) }

        intensityUpdateJob?.cancel()
        intensityUpdateJob = viewModelScope.launch {
            delay(20) // Debounce physical Glyph update
            val intensityMap = getIntensitiesMap()
            glyphController.applyGlyphStateWithIntensities(intensityMap, 2000, GlyphController.GlyphOwner.COMPOSER)
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
        if (glyphController.activeOwner.value != GlyphController.GlyphOwner.COMPOSER) {
            glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER)
        }
        _uiState.update { it.copy(durationMs = newDuration) }
    }

    fun addStep() {
        if (glyphController.activeOwner.value != GlyphController.GlyphOwner.COMPOSER) {
            glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER)
        }
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
            glyphController.applyGlyphStateWithIntensities(step.channelIntensities, 2000, GlyphController.GlyphOwner.COMPOSER)
        }
    }

    fun clearSequence() {
        if (glyphController.isPlaying.value) return
        _uiState.update { it.copy(currentSequenceSteps = emptyList()) }
    }

    fun turnOffAllGlyphs() {
        // Stop playback service if running
        getApplication<Application>().stopService(
            Intent(getApplication(), GlyphPlaybackService::class.java)
        )

        glyphController.stopPlayback()
        glyphController.turnOffGlyphs()
        _uiState.update {
            it.copy(
                glyphIntensities = listOf(0, 0, 0, 0, 0, 0, 0),
                activePlaylistId = null,
                activePresetName = null // Ensure everything is reset
            )
        }
    }

    /**
     * Called when the Composer screen is entered.
     * Ensures hardware is clean and then synced to the current UI state.
     */
    fun onEnterComposer() {
        viewModelScope.launch {
            // 1. Acquire control for stability
            glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER)
            
            // 2. Kill all hardware lights for a clean slate
            glyphController.turnOffGlyphs()
            
            // 3. Small delay to ensure hardware is ready for next command
            delay(100)
            
            // 4. Read the current UI state and push it to hardware
            val intensities = getIntensitiesMap()
            glyphController.applyGlyphStateWithIntensities(
                intensities, 
                durationMs = 2000, 
                owner = GlyphController.GlyphOwner.COMPOSER
            )
        }
    }

    fun togglePause() {
        glyphController.togglePausePlayback()
    }

    fun stopPlayback() {
        glyphController.stopPlayback()
        glyphController.releaseControl(GlyphController.GlyphOwner.COMPOSER)
        _uiState.update { it.copy(activePlaylistId = null, activePresetName = null) }
    }

    fun startPlayback(steps: List<GlyphSequence>, playlistId: Long? = null, presetName: String? = null) {
        if (steps.isEmpty()) return
        glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER)
        glyphController.playSequence(steps, loop = true, id = playlistId, name = presetName, owner = GlyphController.GlyphOwner.COMPOSER)
        _uiState.update { it.copy(activePlaylistId = playlistId, activePresetName = presetName) }
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
            startPlayback(steps, playlist.playlist.id, playlist.playlist.name)
        }
    }

    private fun getIntensitiesMap(): Map<Int, Int> =
        channels.mapIndexed { index, ch -> ch to _uiState.value.glyphIntensities[index] }.toMap()

    override fun onCleared() {
        super.onCleared()
        glyphController.releaseControl(GlyphController.GlyphOwner.COMPOSER)
        glyphController.turnOffGlyphs()
    }
}
