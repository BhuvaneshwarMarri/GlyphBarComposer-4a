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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

data class PatternLabUiState(
    val selectedPlaylistA: PlaylistWithSteps? = null,
    val selectedPlaylistB: PlaylistWithSteps? = null,
    val speedMultiplierA: Float = 1.0f,
    val speedMultiplierB: Float = 1.0f,
    val repeatsA: Int = 1,
    val repeatsB: Int = 1,
    val brightnessA: Float = 1.0f,
    val brightnessB: Float = 1.0f,
    val isInvertedA: Boolean = false,
    val isInvertedB: Boolean = false,
    val isMirroredA: Boolean = false,
    val isMirroredB: Boolean = false,
    val isLayered: Boolean = false,
    val isPlaying: Boolean = false,
    val resultName: String = "",
    val previewSteps: List<GlyphSequence> = emptyList()
)

class PatternLabViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val glyphController = GlyphController.getInstance(application)

    private val _uiState = MutableStateFlow(PatternLabUiState())
    val uiState: StateFlow<PatternLabUiState> = _uiState.asStateFlow()

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

    private val mirrorMap = mapOf(
        Glyph.Code_25111.A_1 to Glyph.Code_25111.A_6,
        Glyph.Code_25111.A_2 to Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_3 to Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_4 to Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_5 to Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_6 to Glyph.Code_25111.A_1
    )

    fun selectPlaylistA(playlist: PlaylistWithSteps?) {
        _uiState.update { it.copy(selectedPlaylistA = playlist) }
        updatePreview()
    }

    fun selectPlaylistB(playlist: PlaylistWithSteps?) {
        _uiState.update { it.copy(selectedPlaylistB = playlist) }
        updatePreview()
    }

    fun onSpeedMultiplierAChange(multiplier: Float) {
        _uiState.update { it.copy(speedMultiplierA = multiplier) }
        updatePreview()
    }

    fun onSpeedMultiplierBChange(multiplier: Float) {
        _uiState.update { it.copy(speedMultiplierB = multiplier) }
        updatePreview()
    }

    fun onRepeatsAChange(repeats: Int) {
        _uiState.update { it.copy(repeatsA = repeats) }
        updatePreview()
    }

    fun onRepeatsBChange(repeats: Int) {
        _uiState.update { it.copy(repeatsB = repeats) }
        updatePreview()
    }

    fun onBrightnessAChange(brightness: Float) {
        _uiState.update { it.copy(brightnessA = brightness) }
        updatePreview()
    }

    fun onBrightnessBChange(brightness: Float) {
        _uiState.update { it.copy(brightnessB = brightness) }
        updatePreview()
    }

    fun onInvertAChange(inverted: Boolean) {
        _uiState.update { it.copy(isInvertedA = inverted) }
        updatePreview()
    }

    fun onInvertBChange(inverted: Boolean) {
        _uiState.update { it.copy(isInvertedB = inverted) }
        updatePreview()
    }

    fun onMirrorAChange(mirrored: Boolean) {
        _uiState.update { it.copy(isMirroredA = mirrored) }
        updatePreview()
    }

    fun onMirrorBChange(mirrored: Boolean) {
        _uiState.update { it.copy(isMirroredB = mirrored) }
        updatePreview()
    }

    fun onMergeModeChange(layered: Boolean) {
        _uiState.update { it.copy(isLayered = layered) }
        updatePreview()
    }

    fun onResultNameChange(name: String) {
        _uiState.update { it.copy(resultName = name) }
    }

    private fun invertIntensity(intensity: Int): Int = when (intensity) {
        3 -> 0
        2 -> 1
        1 -> 2
        else -> 3
    }

    private fun processSteps(
        steps: List<SequenceStep>, 
        multiplier: Float, 
        inverted: Boolean, 
        mirrored: Boolean,
        brightness: Float
    ): List<GlyphSequence> {
        val speedFactor = 1.0f / multiplier
        return steps.map { step ->
            var processedIntensities = step.channelIntensities.mapValues { entry ->
                var value = entry.value
                
                // 1. Invert
                if (inverted) value = invertIntensity(value)
                
                // 2. Brightness scaling
                if (brightness != 1.0f) {
                    value = (value * brightness).roundToInt().coerceIn(0, 3)
                }
                
                value
            }

            // 3. Mirror
            if (mirrored) {
                val mirroredIntensities = mutableMapOf<Int, Int>()
                processedIntensities.forEach { (ch, intensity) ->
                    val targetCh = mirrorMap[ch] ?: ch
                    mirroredIntensities[targetCh] = intensity
                }
                processedIntensities = mirroredIntensities
            }

            GlyphSequence(processedIntensities, (step.durationMs * speedFactor).toInt())
        }
    }

    private fun updatePreview() {
        val state = _uiState.value
        val a = state.selectedPlaylistA
        val b = state.selectedPlaylistB
        
        if (a == null && b == null) {
            _uiState.update { it.copy(previewSteps = emptyList()) }
            return
        }

        val stepsA = a?.steps?.sortedBy { it.stepIndex } ?: emptyList()
        val stepsB = b?.steps?.sortedBy { it.stepIndex } ?: emptyList()

        val baseProcessedA = processSteps(stepsA, state.speedMultiplierA, state.isInvertedA, state.isMirroredA, state.brightnessA)
        val baseProcessedB = processSteps(stepsB, state.speedMultiplierB, state.isInvertedB, state.isMirroredB, state.brightnessB)

        // Apply repeats
        val processedA = mutableListOf<GlyphSequence>().apply {
            repeat(state.repeatsA) { addAll(baseProcessedA) }
        }
        val processedB = mutableListOf<GlyphSequence>().apply {
            repeat(state.repeatsB) { addAll(baseProcessedB) }
        }

        val resultSteps = if (state.isLayered) {
            val maxLen = max(processedA.size, processedB.size)
            (0 until maxLen).map { i ->
                val stepA = processedA.getOrNull(i)
                val stepB = processedB.getOrNull(i)
                val combinedIntensities = mutableMapOf<Int, Int>()
                val allChannels = (stepA?.channelIntensities?.keys ?: emptySet()) + 
                                 (stepB?.channelIntensities?.keys ?: emptySet())
                
                allChannels.forEach { ch ->
                    val valA = stepA?.channelIntensities?.get(ch) ?: 0
                    val valB = stepB?.channelIntensities?.get(ch) ?: 0
                    combinedIntensities[ch] = max(valA, valB)
                }
                
                val duration = when {
                    stepA != null && stepB != null -> max(stepA.durationMs, stepB.durationMs)
                    stepA != null -> stepA.durationMs
                    else -> stepB!!.durationMs
                }
                GlyphSequence(combinedIntensities, duration)
            }
        } else {
            processedA + processedB
        }
        _uiState.update { it.copy(previewSteps = resultSteps) }
    }

    fun togglePreview() {
        if (_uiState.value.isPlaying) stopPreview() else startPreview()
    }

    private fun startPreview() {
        val steps = _uiState.value.previewSteps
        if (steps.isEmpty()) return
        stopPreview()
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob = viewModelScope.launch {
            try {
                while (true) {
                    for (step in steps) {
                        glyphController.applyGlyphStateWithIntensities(step.channelIntensities, step.durationMs)
                        delay(step.durationMs.toLong() + 50)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun stopPreview() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { it.copy(isPlaying = false) }
        glyphController.turnOffGlyphs()
    }

    fun saveResult() {
        val state = _uiState.value
        if (state.resultName.isBlank() || state.previewSteps.isEmpty()) return
        viewModelScope.launch {
            val playlist = Playlist(name = state.resultName)
            val playlistId = playlistDao.insertPlaylist(playlist)
            val sequenceSteps = state.previewSteps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = playlistId,
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            playlistDao.insertSteps(sequenceSteps)
            _uiState.update { it.copy(
                resultName = "", 
                selectedPlaylistA = null, 
                selectedPlaylistB = null, 
                previewSteps = emptyList(),
                isInvertedA = false,
                isInvertedB = false,
                isMirroredA = false,
                isMirroredB = false,
                repeatsA = 1,
                repeatsB = 1,
                brightnessA = 1.0f,
                brightnessB = 1.0f,
                speedMultiplierA = 1.0f,
                speedMultiplierB = 1.0f
            ) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
