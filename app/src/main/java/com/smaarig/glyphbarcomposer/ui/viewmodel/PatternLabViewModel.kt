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
import java.util.Random
import kotlin.math.max
import kotlin.math.roundToInt

enum class LabBlendMode { MAX, ADD, CROSSFADE }

data class PatternLabUiState(
    val selectedPlaylistA: PlaylistWithSteps? = null,
    val selectedPlaylistB: PlaylistWithSteps? = null,
    val speedMultiplierA: Float = 1.0f,
    val speedMultiplierB: Float = 1.0f,
    val isInvertedA: Boolean = false,
    val isInvertedB: Boolean = false,
    val isMirroredA: Boolean = false,
    val isMirroredB: Boolean = false,
    val isReversedA: Boolean = false,
    val isReversedB: Boolean = false,
    val isPingPongA: Boolean = false,
    val isPingPongB: Boolean = false,
    val isLayered: Boolean = false,
    val offsetB: Int = 0,
    val blendMode: LabBlendMode = LabBlendMode.MAX,
    val crossfadeRatio: Float = 0.5f,
    val isPlaying: Boolean = false,
    val resultName: String = "",
    val previewSteps: List<GlyphSequence> = emptyList(),
    val currentPreviewIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val selectedTab: Int = 0
)

class PatternLabViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {
    private val glyphController = GlyphController.getInstance(application)

    private val _uiState = MutableStateFlow(PatternLabUiState())
    val uiState: StateFlow<PatternLabUiState> = _uiState.asStateFlow()

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

    fun onTabChange(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        if (_uiState.value.isPlaying) {
            startPreview() // Restart with new tab's steps
        }
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

    fun onReverseAChange(reversed: Boolean) {
        _uiState.update { it.copy(isReversedA = reversed) }
        updatePreview()
    }

    fun onReverseBChange(reversed: Boolean) {
        _uiState.update { it.copy(isReversedB = reversed) }
        updatePreview()
    }

    fun onPingPongAChange(pingPong: Boolean) {
        _uiState.update { it.copy(isPingPongA = pingPong) }
        updatePreview()
    }

    fun onPingPongBChange(pingPong: Boolean) {
        _uiState.update { it.copy(isPingPongB = pingPong) }
        updatePreview()
    }

    fun onOffsetBChange(offset: Int) {
        _uiState.update { it.copy(offsetB = offset) }
        updatePreview()
    }

    fun onBlendModeChange(mode: LabBlendMode) {
        _uiState.update { it.copy(blendMode = mode) }
        updatePreview()
    }

    fun onCrossfadeRatioChange(ratio: Float) {
        _uiState.update { it.copy(crossfadeRatio = ratio) }
        updatePreview()
    }

    fun randomize() {
        val random = Random()
        _uiState.update { state ->
            state.copy(
                speedMultiplierA = 0.5f + (random.nextFloat() * 1.5f),
                speedMultiplierB = 0.5f + (random.nextFloat() * 1.5f),
                isInvertedA = random.nextBoolean(),
                isInvertedB = random.nextBoolean(),
                isMirroredA = random.nextBoolean(),
                isMirroredB = random.nextBoolean(),
                isReversedA = random.nextBoolean(),
                isReversedB = random.nextBoolean(),
                isPingPongA = random.nextBoolean(),
                isPingPongB = random.nextBoolean(),
                offsetB = if (state.isLayered) random.nextInt(1001) else 0,
                blendMode = LabBlendMode.values().let { it[random.nextInt(it.size)] }
            )
        }
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
        reversed: Boolean,
        pingPong: Boolean
    ): List<GlyphSequence> {
        val speedFactor = 1.0f / multiplier
        val processed = steps.sortedBy { it.stepIndex }.map { step ->
            var intensities = step.channelIntensities.mapValues { entry ->
                var v = entry.value
                if (inverted) v = invertIntensity(v)
                v
            }
            if (mirrored) {
                val mirroredMap = mutableMapOf<Int, Int>()
                intensities.forEach { (ch, intensity) ->
                    mirroredMap[mirrorMap[ch] ?: ch] = intensity
                }
                intensities = mirroredMap
            }
            GlyphSequence(intensities, (step.durationMs * speedFactor).toInt())
        }

        val base = if (reversed) processed.reversed() else processed
        return if (pingPong && base.size > 1) {
            // Smooth bounce: avoid repeating start/end steps
            // For 2 steps [1, 2]: [1, 2, 1] (repeats 1 on loop, but shows return)
            // For 3 steps [1, 2, 3]: [1, 2, 3, 2] (smooth 1-2-3-2-1...)
            if (base.size == 2) base + base.reversed().drop(1)
            else base + base.reversed().drop(1).dropLast(1)
        } else {
            base
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

        val stepsA = a?.steps ?: emptyList()
        val stepsB = b?.steps ?: emptyList()

        val processedA = processSteps(stepsA, state.speedMultiplierA, state.isInvertedA, state.isMirroredA, state.isReversedA, state.isPingPongA)
        val processedB = processSteps(stepsB, state.speedMultiplierB, state.isInvertedB, state.isMirroredB, state.isReversedB, state.isPingPongB)

        val resultSteps = if (state.isLayered) {
            // Add offset to B
            val offsetSteps = if (state.offsetB > 0) {
                listOf(GlyphSequence(emptyMap(), state.offsetB)) + processedB
            } else processedB

            // Normalize both to common time slices (simple version: find a common tick or just blend overlapping)
            // For a robust blend, we'd need to resample. 
            // Minimal version: Just append or use the layered logic with max length
            val maxLen = max(processedA.size, offsetSteps.size)
            (0 until maxLen).map { i ->
                val sA = processedA.getOrNull(i)
                val sB = offsetSteps.getOrNull(i)
                val combined = mutableMapOf<Int, Int>()
                val allChannels = (sA?.channelIntensities?.keys ?: emptySet()) + (sB?.channelIntensities?.keys ?: emptySet())
                
                allChannels.forEach { ch ->
                    val vA = sA?.channelIntensities?.get(ch) ?: 0
                    val vB = sB?.channelIntensities?.get(ch) ?: 0
                    
                    combined[ch] = when(state.blendMode) {
                        LabBlendMode.MAX -> max(vA, vB)
                        LabBlendMode.ADD -> (vA + vB).coerceIn(0, 3)
                        LabBlendMode.CROSSFADE -> {
                            val ratio = state.crossfadeRatio
                            ((vA * (1f - ratio)) + (vB * ratio)).roundToInt().coerceIn(0, 3)
                        }
                    }
                }
                GlyphSequence(combined, max(sA?.durationMs ?: 0, sB?.durationMs ?: 0))
            }
        } else {
            processedA + processedB
        }
        _uiState.update { it.copy(previewSteps = resultSteps) }
    }

    private fun getTabSpecificSteps(): List<GlyphSequence> {
        val state = _uiState.value
        return when (state.selectedTab) {
            0 -> {
                val a = state.selectedPlaylistA
                if (a == null) emptyList()
                else processSteps(a.steps, state.speedMultiplierA, state.isInvertedA, state.isMirroredA, state.isReversedA, state.isPingPongA)
            }
            1 -> {
                val b = state.selectedPlaylistB
                if (b == null) emptyList()
                else processSteps(b.steps, state.speedMultiplierB, state.isInvertedB, state.isMirroredB, state.isReversedB, state.isPingPongB)
            }
            else -> state.previewSteps
        }
    }

    fun togglePreview() {
        if (_uiState.value.isPlaying) stopPreview() else startPreview()
    }

    private fun startPreview() {
        val steps = getTabSpecificSteps()
        if (steps.isEmpty()) return
        stopPreview()
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob = viewModelScope.launch {
            try {
                while (true) {
                    for (step in steps) {
                        _uiState.update { state ->
                            val newList = channels.map { ch -> step.channelIntensities[ch] ?: 0 }
                            state.copy(currentPreviewIntensities = newList)
                        }
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
        _uiState.update { it.copy(isPlaying = false, currentPreviewIntensities = listOf(0, 0, 0, 0, 0, 0, 0)) }
        glyphController.turnOffGlyphs()
    }

    fun saveResult(name: String) {
        val state = _uiState.value
        if (name.isBlank() || state.previewSteps.isEmpty()) return
        viewModelScope.launch {
            val playlist = Playlist(name = name)
            val sequenceSteps = state.previewSteps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = 0,
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            repository.savePlaylist(playlist, sequenceSteps)
            _uiState.update { it.copy(
                resultName = "", 
                selectedPlaylistA = null, 
                selectedPlaylistB = null, 
                previewSteps = emptyList(),
                isInvertedA = false,
                isInvertedB = false,
                isMirroredA = false,
                isMirroredB = false,
                isReversedA = false,
                isReversedB = false,
                isPingPongA = false,
                isPingPongB = false,
                speedMultiplierA = 1.0f,
                speedMultiplierB = 1.0f,
                isLayered = false,
                offsetB = 0,
                blendMode = LabBlendMode.MAX
            ) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
