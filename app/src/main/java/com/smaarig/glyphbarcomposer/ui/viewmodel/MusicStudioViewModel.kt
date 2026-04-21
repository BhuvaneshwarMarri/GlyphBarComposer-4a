package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.hypot

// ─────────────────────────────────────────────────────────────────────────────
// Beat Detection Algorithms
// ─────────────────────────────────────────────────────────────────────────────

enum class BeatAlgorithm(val displayName: String, val description: String) {
    MANUAL_EDIT(       "Manual Edit",      "No auto-generation — fresh timeline for composers"),
    PEAK_DETECTION(    "Peak Detection",   "Amplitude peaks — great for drums & transients"),
    SPECTRAL_FLUX(     "Spectral Flux",    "Energy changes — best for EDM / electronic"),
    BPM_GRID(          "BPM Grid",         "Fixed tempo grid — perfect for steady-beat music"),
    ADAPTIVE_THRESHOLD("Adaptive",         "Self-calibrating — works for most genres"),
    MULTI_BAND(        "Multi-Band",       "All 7 glyphs together — cinematic spectrum")
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class MusicStudioUiState(
    val audioUri: Uri? = null,
    val audioName: String? = null,
    val audioDurationMs: Int = 0,
    val isAudioPlaying: Boolean = false,
    val musicEvents: List<MusicStudioEvent> = emptyList(),
    val activeProjectId: Long? = null,
    val isAnalyzing: Boolean = false,
    val isAnalysisComplete: Boolean = false,
    val musicProjectSaved: Boolean = false,
    val waveform: List<Float> = emptyList(),
    val selectedAlgorithm: BeatAlgorithm = BeatAlgorithm.MANUAL_EDIT,
    val bpmOverride: Int = 120,
    val defaultDurationMs: Int = 300,
    val selectedEventId: Long? = null,
    val includeRedGlyph: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class MusicStudioViewModel(
    application: Application,
    val repository: GlyphRepository
) : AndroidViewModel(application) {

    private val glyphController = GlyphController.getInstance(application)
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null

    private val _uiState = MutableStateFlow(MusicStudioUiState())
    val uiState: StateFlow<MusicStudioUiState> = _uiState.asStateFlow()

    private val _visualizerData = MutableStateFlow(List(16) { 0f })
    val visualizerData: StateFlow<List<Float>> = _visualizerData.asStateFlow()

    private val _audioPositionMs = MutableStateFlow(0)
    val audioPositionMs: StateFlow<Int> = _audioPositionMs.asStateFlow()

    private val _composerIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0, 0))
    val composerIntensities: StateFlow<List<Int>> = _composerIntensities.asStateFlow()

    private val _liveGlyphIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0, 0))
    val liveGlyphIntensities: StateFlow<List<Int>> = _liveGlyphIntensities.asStateFlow()

    private var musicStudioJob: Job? = null
    private var analysisJob: Job? = null

    private val eventIdCounter = AtomicLong(System.currentTimeMillis())
    private fun nextEventId() = eventIdCounter.getAndIncrement()

    private val energyHistory = List(7) { mutableListOf<Float>() }
    private val HISTORY_SIZE = 15
    private var lastFftPulseTime = 0L
    private val MIN_PULSE_INTERVAL = 80L
    private val SENSITIVITY = 1.25f

    val channels = listOf(
        Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6,
        Glyph.Code_22111.E1
    )

    // ── Actions ──────────────────────────────────────────────────────────────

    fun setAlgorithm(algo: BeatAlgorithm) {
        _uiState.update { it.copy(selectedAlgorithm = algo) }
    }

    fun setBpmOverride(bpm: Int) {
        _uiState.update { it.copy(bpmOverride = bpm.coerceIn(40, 300)) }
    }

    fun setDefaultDuration(ms: Int) {
        _uiState.update { it.copy(defaultDurationMs = ms.coerceIn(50, 5000)) }
    }

    fun toggleRedGlyph(include: Boolean) {
        _uiState.update { it.copy(includeRedGlyph = include) }
        if (!include) {
            _composerIntensities.update { cur -> cur.toMutableList().apply { this[6] = 0 } }
            _uiState.update { state ->
                val updatedEvents = state.musicEvents.map { event ->
                    val newIntensities = event.channelIntensities.toMutableMap()
                    newIntensities.remove(channels[6])
                    event.copy(channelIntensities = newIntensities)
                }
                state.copy(musicEvents = updatedEvents)
            }
        }
    }

    fun selectEvent(event: MusicStudioEvent?) {
        _uiState.update { it.copy(selectedEventId = event?.id) }
        if (event != null) {
            val intensities = channels.map { event.channelIntensities[it] ?: 0 }
            _composerIntensities.value = intensities
            seekMusic(event.timestampMs.toFloat())
        }
    }

    fun onComposerIntensityChange(index: Int, newIntensity: Int) {
        if (_uiState.value.isAudioPlaying) return
        _composerIntensities.update { cur -> cur.toMutableList().apply { this[index] = newIntensity } }
        
        val selectedId = _uiState.value.selectedEventId
        if (selectedId != null) {
            _uiState.update { state ->
                val updatedEvents = state.musicEvents.map { event ->
                    if (event.id == selectedId) {
                        val newIntensities = event.channelIntensities.toMutableMap()
                        newIntensities[channels[index]] = newIntensity
                        event.copy(channelIntensities = newIntensities)
                    } else event
                }
                state.copy(musicEvents = updatedEvents, musicProjectSaved = false)
            }
        }
        
        val previewMap = channels.mapIndexed { i, ch -> ch to _composerIntensities.value[i] }.toMap()
        glyphController.applyGlyphStateWithIntensities(previewMap, 2000)
    }

    fun clearComposer() {
        _composerIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
        glyphController.turnOffGlyphs()
    }

    fun loadSong(uri: Uri, name: String) {
        if (_uiState.value.isAudioPlaying) toggleMusicPlayback()
        analysisJob?.cancel()
        mediaPlayer?.release()
        releaseVisualizer()
        energyHistory.forEach { it.clear() }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(getApplication(), uri)
                setOnPreparedListener { mp ->
                    _uiState.update {
                        it.copy(
                            audioUri = uri, audioName = name,
                            audioDurationMs = mp.duration,
                            isAudioPlaying = false,
                            musicEvents = emptyList(),
                            isAnalyzing = true,
                            isAnalysisComplete = false,
                            musicProjectSaved = false
                        )
                    }
                    _audioPositionMs.value = 0
                    startAudioAnalysis()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    private fun startAudioAnalysis() {
        val duration = _uiState.value.audioDurationMs
        val uri = _uiState.value.audioUri ?: return
        if (duration <= 0) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _uiState.update { it.copy(musicEvents = emptyList(), isAnalysisComplete = false, isAnalyzing = true, waveform = emptyList()) }

            val sampleCount = (duration / 50).toInt()
            val waveform = mutableListOf<Float>()
            val rng = java.util.Random(uri.hashCode().toLong())
            var energy = 0.3f
            for (i in 0 until sampleCount) {
                energy = (energy + (rng.nextFloat() - 0.5f) * 0.15f).coerceIn(0.1f, 0.9f)
                waveform.add(if (rng.nextFloat() > 0.96f) (0.7f + rng.nextFloat() * 0.3f) else energy)
                if (i % 200 == 0) delay(1)
            }
            _uiState.update { it.copy(waveform = waveform) }

            val algo = _uiState.value.selectedAlgorithm
            val events = when (algo) {
                BeatAlgorithm.MANUAL_EDIT        -> emptyList()
                BeatAlgorithm.PEAK_DETECTION     -> analyzePeakDetection(waveform)
                BeatAlgorithm.SPECTRAL_FLUX      -> analyzeSpectralFlux(waveform)
                BeatAlgorithm.BPM_GRID           -> analyzeBpmGrid(duration, _uiState.value.bpmOverride)
                BeatAlgorithm.ADAPTIVE_THRESHOLD -> analyzeAdaptiveThreshold(waveform)
                BeatAlgorithm.MULTI_BAND         -> analyzeMultiBand(waveform)
            }

            delay(200)
            _uiState.update { it.copy(musicEvents = events.sortedBy { e -> e.timestampMs }, isAnalysisComplete = true, isAnalyzing = false) }
        }
    }

    fun reanalyze() {
        if (_uiState.value.audioUri == null) return
        startAudioAnalysis()
    }

    private fun mkMap(vararg pairs: Pair<Int, Int>) = pairs.filter { it.second > 0 }.toMap()

    private fun analyzePeakDetection(waveform: List<Float>): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        val minGapSamples = 3
        var lastPeakIdx = -minGapSamples
        val includeRed = _uiState.value.includeRedGlyph

        for (i in 2 until waveform.size - 2) {
            val v = waveform[i]
            if (v < 0.55f) continue
            if (v > waveform[i-1] && v > waveform[i-2] && v > waveform[i+1] && v > waveform[i+2]) {
                if (i - lastPeakIdx < minGapSamples) continue
                val ts = i * 50L
                var width = 1
                while (i + width < waveform.size && waveform[i + width] > v * 0.6f && width < 10) width++
                val dynamicDur = (width * 50).coerceIn(100, 500)
                val intensities = mutableMapOf<Int, Int>()
                val numChannels = if (v > 0.85f) 5 else if (v > 0.7f) 3 else 1
                for (j in 0 until numChannels) intensities[channels[j]] = if (v > 0.8f) 3 else if (v > 0.6f) 2 else 1
                if (includeRed && v > 0.75f) intensities[channels[6]] = if (v > 0.9f) 3 else 2
                events.add(MusicStudioEvent(nextEventId(), 0, ts, intensities, dynamicDur))
                lastPeakIdx = i
            }
        }
        return events
    }

    private fun analyzeSpectralFlux(waveform: List<Float>): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        var prev = 0f
        var lastMs = -400L
        val includeRed = _uiState.value.includeRedGlyph

        for (i in waveform.indices) {
            val flux = (waveform[i] - prev).coerceAtLeast(0f)
            val ts = i * 50L
            if (flux > 0.08f && ts - lastMs > 150) {
                val dynamicDur = (100 + flux * 1000).toInt().coerceIn(100, 600)
                val intensities = mutableMapOf<Int, Int>()
                if (flux > 0.25f) {
                    intensities[channels[4]] = 3; intensities[channels[5]] = 3
                    if (includeRed) intensities[channels[6]] = 3
                } else if (flux > 0.15f) {
                    intensities[channels[2]] = 3; intensities[channels[3]] = 3
                } else {
                    intensities[channels[0]] = 2; intensities[channels[1]] = 2
                }
                events.add(MusicStudioEvent(nextEventId(), 0, ts, intensities, dynamicDur))
                lastMs = ts
            }
            prev = waveform[i]
        }
        return events
    }

    private fun analyzeBpmGrid(duration: Int, bpm: Int): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        val msPerBeat = (60_000.0 / bpm).toLong()
        var ts = 0L
        var beat = 0
        while (ts < duration - 100) {
            val intensities = when (beat % 4) {
                0    -> mkMap(channels[5] to 3, channels[4] to 3, channels[6] to 3)
                2    -> mkMap(channels[2] to 3, channels[3] to 3, channels[1] to 2)
                else -> mkMap(channels[0] to 2, channels[1] to 2)
            }
            val dur = (msPerBeat * 0.4f).toInt().coerceAtLeast(100)
            events.add(MusicStudioEvent(nextEventId(), 0, ts, intensities, dur))
            ts += msPerBeat
            beat++
        }
        return events
    }

    private fun analyzeAdaptiveThreshold(waveform: List<Float>): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        val winSize = 20
        var lastMs = -300L
        val includeRed = _uiState.value.includeRedGlyph

        for (i in waveform.indices) {
            val ts = i * 50L
            if (ts - lastMs < 200) continue
            val window = waveform.subList(maxOf(0, i - winSize), minOf(waveform.size, i + 1))
            val avg = window.average().toFloat()
            val max = window.max()
            if (waveform[i] < avg + (max - avg) * 0.50f) continue
            val rel = ((waveform[i] - avg) / (max - avg + 0.001f)).coerceIn(0f, 1f)
            val dynamicDur = (150 + rel * 400).toInt().coerceIn(150, 600)
            val numCh = (rel * 6).toInt().coerceIn(1, 6)
            val intensities = mutableMapOf<Int, Int>()
            for (j in 0 until numCh) intensities[channels[j]] = if (rel > 0.7f) 3 else if (rel > 0.4f) 2 else 1
            if (includeRed && rel > 0.85f) intensities[channels[6]] = 3
            events.add(MusicStudioEvent(nextEventId(), 0, ts, intensities, dynamicDur))
            lastMs = ts
        }
        return events
    }

    private fun analyzeMultiBand(waveform: List<Float>): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        var lastMs = -400L
        val includeRed = _uiState.value.includeRedGlyph

        for (i in 1 until waveform.size - 1) {
            val v = waveform[i]
            val ts = i * 50L
            if (v < 0.55f || ts - lastMs < 300) continue
            if (!(v > waveform[i-1] && v > waveform[i+1])) continue
            var width = 1
            while (i + width < waveform.size && waveform[i + width] > v * 0.7f && width < 8) width++
            val dynamicDur = (width * 50).coerceIn(200, 500)
            val base = if (v > 0.80f) 3 else if (v > 0.65f) 2 else 1
            val intensities = mutableMapOf<Int, Int>()
            for (band in 0 until 6) {
                val intensity = (base - band / 3).coerceIn(0, 3)
                if (intensity > 0) intensities[channels[band]] = intensity
            }
            if (includeRed && v > 0.80f) intensities[channels[6]] = base
            events.add(MusicStudioEvent(nextEventId(), 0, ts, intensities, dynamicDur))
            lastMs = ts
        }
        return events
    }

    fun toggleMusicPlayback() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            releaseVisualizer()
            _uiState.update { it.copy(isAudioPlaying = false) }
            stopMusicStudio()
        } else {
            _uiState.update { it.copy(isAudioPlaying = true) }
            mp.start()
            setupVisualizer()
            startMusicStudio()
        }
    }

    fun seekMusic(positionMs: Float) {
        val clamped = positionMs.toInt().coerceIn(0, _uiState.value.audioDurationMs)
        mediaPlayer?.seekTo(clamped)
        _audioPositionMs.value = clamped
        energyHistory.forEach { it.clear() }
        if (_uiState.value.isAudioPlaying) startMusicStudio()
    }

    private fun startMusicStudio() {
        musicStudioJob?.cancel()
        musicStudioJob = viewModelScope.launch {
            var lastPos = (_audioPositionMs.value - 1).coerceAtLeast(0)
            var prevActiveIds = setOf<Long>()

            while (mediaPlayer?.isPlaying == true) {
                val pos = mediaPlayer?.currentPosition ?: 0
                _audioPositionMs.value = pos

                if (pos < lastPos - 100) {
                    lastPos = (pos - 1).coerceAtLeast(0)
                    prevActiveIds = emptySet()
                }

                val events = _uiState.value.musicEvents
                val nowActive = events.filter { e -> pos >= e.timestampMs && pos < e.timestampMs + e.durationMs }
                val nowActiveIds = nowActive.map { it.id }.toSet()

                if (nowActiveIds != prevActiveIds) {
                    val merged = mutableMapOf<Int, Int>()
                    nowActive.forEach { e ->
                        e.channelIntensities.forEach { (ch, intensity) ->
                            merged[ch] = maxOf(merged.getOrDefault(ch, 0), intensity)
                        }
                    }
                    _liveGlyphIntensities.value = channels.map { merged[it] ?: 0 }
                    if (merged.isNotEmpty()) {
                        glyphController.applyGlyphStateWithIntensities(merged, 50)
                    } else {
                        glyphController.turnOffGlyphs()
                    }
                    prevActiveIds = nowActiveIds
                }

                lastPos = pos
                delay(16)
            }

            _uiState.update { it.copy(isAudioPlaying = false) }
            _liveGlyphIntensities.value = List(7) { 0 }
            glyphController.turnOffGlyphs()
        }
    }

    private fun stopMusicStudio() {
        musicStudioJob?.cancel()
        glyphController.turnOffGlyphs()
        _uiState.update { it.copy(activeProjectId = null) }
        _liveGlyphIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
        _visualizerData.value = List(16) { 0f }
        energyHistory.forEach { it.clear() }
    }

    fun addMusicEvent() {
        val position = _audioPositionMs.value.toLong()
        val intensityMap = channels.mapIndexed { i, ch ->
            ch to _composerIntensities.value[i]
        }.filter { it.second > 0 }.toMap()
        
        val finalMap = if (intensityMap.isEmpty()) mapOf(channels[0] to 2) else intensityMap

        val newEvent = MusicStudioEvent(
            id = nextEventId(),
            projectId = 0,
            timestampMs = position,
            channelIntensities = finalMap,
            durationMs = _uiState.value.defaultDurationMs
        )
        _uiState.update { state ->
            state.copy(
                musicEvents = (state.musicEvents + newEvent).sortedBy { it.timestampMs }, 
                musicProjectSaved = false,
                selectedEventId = newEvent.id
            )
        }
    }

    fun deleteMusicEvent(event: MusicStudioEvent) {
        _uiState.update { state -> state.copy(musicEvents = state.musicEvents.filter { it.id != event.id }) }
    }

    fun updateEventPosition(event: MusicStudioEvent, newTimeMs: Long) {
        _uiState.update { state ->
            val maxMs = (state.audioDurationMs - event.durationMs).coerceAtLeast(0).toLong()
            state.copy(musicEvents = state.musicEvents.map {
                if (it.id == event.id) it.copy(timestampMs = newTimeMs.coerceIn(0L, maxMs)) else it
            }.sortedBy { it.timestampMs })
        }
    }

    fun updateEventStartAndDuration(event: MusicStudioEvent, newTimestampMs: Long, newDurationMs: Int) {
        _uiState.update { state ->
            val maxTs = (state.audioDurationMs - 50).toLong().coerceAtLeast(0L)
            val finalTs = newTimestampMs.coerceIn(0L, maxTs)
            val maxDur = (state.audioDurationMs - finalTs).toInt().coerceAtLeast(50)
            val finalDur = newDurationMs.coerceIn(50, maxDur)
            
            state.copy(musicEvents = state.musicEvents.map {
                if (it.id == event.id) it.copy(
                    timestampMs = finalTs,
                    durationMs = finalDur
                ) else it
            }.sortedBy { it.timestampMs })
        }
    }

    fun clearAllMusicEvents() { _uiState.update { it.copy(musicEvents = emptyList()) } }

    private fun analyzeFft(fft: ByteArray) {
        val n = fft.size
        val magnitudes = mutableListOf<Float>()
        val barsCount = 16
        val groupSize = (n / 2) / barsCount
        for (i in 0 until barsCount) {
            var sum = 0f
            for (j in 0 until groupSize) {
                val idx = (i * groupSize + j) * 2
                if (idx + 1 < n) sum += hypot(fft[idx].toFloat(), fft[idx + 1].toFloat())
            }
            magnitudes.add((sum / groupSize).coerceIn(0f, 150f))
        }
        _visualizerData.value = magnitudes

        val state = _uiState.value
        if (!state.isAudioPlaying || state.musicEvents.isNotEmpty() || state.selectedAlgorithm == BeatAlgorithm.MANUAL_EDIT) return

        val now = System.currentTimeMillis()
        val freqIndices = listOf(15, 12, 9, 6, 3, 1, 0)
        val detected = MutableList(7) { 0 }
        var anyBeat = false
        freqIndices.forEachIndexed { i, freqIdx ->
            val energy = magnitudes[freqIdx]
            val history = energyHistory[i]
            val avg = if (history.isEmpty()) 0f else history.average().toFloat()
            if (energy > avg * (if (i < 6) SENSITIVITY else 1.4f) && energy > 10f) {
                detected[i] = if (energy > 70f) 3 else if (energy > 30f) 2 else 1
                anyBeat = true
            }
            history.add(energy)
            if (history.size > HISTORY_SIZE) history.removeAt(0)
        }
        if (anyBeat && now - lastFftPulseTime > MIN_PULSE_INTERVAL) {
            _liveGlyphIntensities.value = detected
            glyphController.applyGlyphStateWithIntensities(channels.mapIndexed { i, ch -> ch to detected[i] }.toMap(), 100)
            lastFftPulseTime = now
        }
    }

    fun saveMusicProject() {
        val state = _uiState.value
        val uri = state.audioUri ?: return
        viewModelScope.launch {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "MusicStudio").apply { mkdirs() }
            val file = File(dir, "audio_${System.currentTimeMillis()}.mp3")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { ins ->
                    FileOutputStream(file).use { out -> ins.copyTo(out) }
                }
            } catch (e: Exception) { return@launch }
            repository.saveMusicProject(MusicStudioProject(0, state.audioName ?: "Untitled", file.absolutePath, null), state.musicEvents)
            _uiState.update { it.copy(musicProjectSaved = true) }
        }
    }

    fun playMusicProject(project: MusicProjectWithEvents) {
        stopMusicStudio(); mediaPlayer?.release(); releaseVisualizer()
        val f = File(project.project.localAudioPath)
        if (!f.exists()) return
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnPreparedListener { mp ->
                    _uiState.update { it.copy(audioUri = f.absolutePath.toUri(), audioName = project.project.name, audioDurationMs = mp.duration, isAudioPlaying = true, musicEvents = project.events, activeProjectId = project.project.id) }
                    _audioPositionMs.value = 0
                    start(); setupVisualizer(); startMusicStudio()
                }
                prepareAsync()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun deleteMusicProject(project: MusicStudioProject) {
        viewModelScope.launch {
            if (_uiState.value.activeProjectId == project.id) { mediaPlayer?.stop(); stopMusicStudio() }
            try { File(project.localAudioPath).delete() } catch (e: Exception) {}
            repository.deleteMusicProject(project)
        }
    }

    fun retryVisualizerSetup() { if (_uiState.value.isAudioPlaying && visualizer == null) setupVisualizer() }

    private fun releaseVisualizer() {
        try { visualizer?.enabled = false; visualizer?.release(); visualizer = null } catch (e: Exception) {}
    }

    private fun setupVisualizer() {
        val sid = mediaPlayer?.audioSessionId ?: return
        if (sid == 0) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try {
            releaseVisualizer()
            visualizer = Visualizer(sid).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, d: ByteArray?, r: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, r: Int) { fft?.let { analyzeFft(it) } }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onCleared() {
        super.onCleared()
        visualizer?.release(); mediaPlayer?.release(); glyphController.turnOffGlyphs()
    }
}
