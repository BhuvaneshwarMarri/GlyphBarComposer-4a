package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.utils.AudioAnalyzer
import com.smaarig.glyphbarcomposer.utils.AudioProcessor
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
    MANUAL_EDIT(
        "FREESTYLE",
        "No auto-generation — fresh timeline for composers"
    ),
    PRO_SYNC_FFT(
        "VIBE CHECK",
        "Mel-scale frequency bands with hysteresis — maps each instrument to its own LED"
    ),
    PEAK_DETECTION(
        "DRUM GO DUM",
        "Median-adaptive transient detector — precise on drums & fast transients"
    ),
    SPECTRAL_FLUX(
        "EDM MAGIC",
        "Per-band half-wave rectified flux — best for EDM, electronic, synth-heavy music"
    ),
    VOLUME_HEIGHT(
        "LOUDNESS",
        "Bar-graph mode — number of lit LEDs grows and shrinks with loudness in real time"
    ),
    BPM_GRID(
        "TEMPO TIK",
        "Onset-snapped tempo grid — fills patterns on every beat for steady-tempo tracks"
    ),
    ADAPTIVE_THRESHOLD(
        "BRAINAF",
        "Local energy ratio threshold — self-calibrates across quiet and loud sections"
    ),
    MULTI_BAND(
        "DANCE FLOOR",
        "7-band equaliser display — all LEDs animate simultaneously as a live spectrum"
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

@Stable
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
    val includeRedGlyph: Boolean = true,
    val isSaving: Boolean = false,
    val showSaveSuccess: Boolean = false
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

    // Live FFT visualizer state (PRO_SYNC_FFT live mode while playing, no saved events)
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

    // ── Actions ───────────────────────────────────────────────────────────────

    fun setAlgorithm(algo: BeatAlgorithm) {
        if (_uiState.value.selectedAlgorithm == algo) return
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
    }

    fun selectEvent(event: MusicStudioEvent?) {
        _uiState.update { it.copy(selectedEventId = event?.id) }
        if (event != null) {
            _composerIntensities.value = channels.map { event.channelIntensities[it] ?: 0 }
            seekMusic(event.timestampMs.toFloat())
        }
    }

    fun onComposerIntensityChange(index: Int, newIntensity: Int) {
        if (_uiState.value.isAudioPlaying) return
        _composerIntensities.update { cur -> cur.toMutableList().apply { this[index] = newIntensity } }

        val selectedId = _uiState.value.selectedEventId
        if (selectedId != null) {
            _uiState.update { state ->
                state.copy(
                    musicEvents = state.musicEvents.map { event ->
                        if (event.id == selectedId) {
                            event.copy(channelIntensities = event.channelIntensities.toMutableMap().apply { put(channels[index], newIntensity) })
                        } else event
                    },
                    musicProjectSaved = false
                )
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
                            audioUri = uri,
                            audioName = name,
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

    // ─────────────────────────────────────────────────────────────────────────
    // Analysis dispatch
    // ─────────────────────────────────────────────────────────────────────────

    private fun startAudioAnalysis() {
        val duration = _uiState.value.audioDurationMs
        val uri      = _uiState.value.audioUri ?: return
        if (duration <= 0) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _uiState.update { it.copy(musicEvents = emptyList(), isAnalysisComplete = false, isAnalyzing = true, waveform = emptyList()) }

            // Waveform for timeline display (RMS energy, fast)
            val waveform = AudioProcessor.extractWaveform(getApplication(), uri, duration)
            _uiState.update { it.copy(waveform = waveform) }

            val algo   = _uiState.value.selectedAlgorithm
            val events = when (algo) {
                BeatAlgorithm.MANUAL_EDIT -> emptyList()

                // ── All algorithms now call AudioAnalyzer and get full FFT maps ──
                BeatAlgorithm.PRO_SYNC_FFT ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeAdvancedDSP(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )

                BeatAlgorithm.PEAK_DETECTION ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzePeakDetection(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )

                BeatAlgorithm.SPECTRAL_FLUX ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeSpectralFlux(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )

                BeatAlgorithm.VOLUME_HEIGHT ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeVolumeHeight(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )

                BeatAlgorithm.BPM_GRID ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeBpmGrid(
                            getApplication(), uri, duration,
                            _uiState.value.bpmOverride, channels
                        ),
                        windowMs = 50
                    )

                BeatAlgorithm.ADAPTIVE_THRESHOLD ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeAdaptiveThreshold(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )

                BeatAlgorithm.MULTI_BAND ->
                    eventsFromIntensityMaps(
                        AudioAnalyzer.analyzeMultiBand(getApplication(), uri, duration, channels),
                        windowMs = 50
                    )
            }

            // Strip events where Red glyph not wanted
            val includeRed = _uiState.value.includeRedGlyph
            val filtered = if (!includeRed) events.map { e ->
                e.copy(channelIntensities = e.channelIntensities.toMutableMap().apply { remove(channels[6]) })
            }.filter { it.channelIntensities.isNotEmpty() }
            else events

            delay(200)
            _uiState.update { it.copy(musicEvents = filtered.sortedBy { e -> e.timestampMs }, isAnalysisComplete = true, isAnalyzing = false) }
        }
    }

    /**
     * Converts a list of per-window intensity maps into MusicStudioEvents.
     * Consecutive identical non-empty maps are merged into a single longer event
     * to reduce event count and keep the timeline readable.
     */
    private fun eventsFromIntensityMaps(
        maps: List<Map<Int, Int>>,
        windowMs: Int
    ): List<MusicStudioEvent> {
        val events = mutableListOf<MusicStudioEvent>()
        if (maps.isEmpty()) return events

        var runStart = -1
        var runMap: Map<Int, Int> = emptyMap()
        var runLen  = 0

        fun flush() {
            if (runStart >= 0 && runMap.isNotEmpty()) {
                events.add(
                    MusicStudioEvent(
                        id = nextEventId(),
                        projectId = 0,
                        timestampMs = runStart * windowMs.toLong(),
                        channelIntensities = runMap,
                        durationMs = (runLen * windowMs).coerceAtLeast(windowMs)
                    )
                )
            }
        }

        maps.forEachIndexed { i, map ->
            if (map.isEmpty()) {
                flush()
                runStart = -1; runMap = emptyMap(); runLen = 0
            } else if (map == runMap && runLen < 10) { // max 500 ms merge
                runLen++
            } else {
                flush()
                runStart = i; runMap = map; runLen = 1
            }
        }
        flush()
        return events
    }

    fun reanalyze() {
        if (_uiState.value.audioUri == null) return
        startAudioAnalysis()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Playback
    // ─────────────────────────────────────────────────────────────────────────

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

                val events     = _uiState.value.musicEvents
                // Increased lookahead/windowing for smoother playback sync
                val nowActive  = events.filter { e -> pos >= e.timestampMs - 20 && pos < e.timestampMs + e.durationMs - 10 }
                val nowIds     = nowActive.map { it.id }.toSet()

                if (nowIds != prevActiveIds) {
                    val merged = mutableMapOf<Int, Int>()
                    nowActive.forEach { e ->
                        e.channelIntensities.forEach { (ch, intensity) ->
                            merged[ch] = maxOf(merged.getOrDefault(ch, 0), intensity)
                        }
                    }
                    _liveGlyphIntensities.value = channels.map { merged[it] ?: 0 }
                    if (merged.isNotEmpty()) glyphController.applyGlyphStateWithIntensities(merged, 50)
                    else glyphController.turnOffGlyphs()
                    prevActiveIds = nowIds
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

    // ─────────────────────────────────────────────────────────────────────────
    // Timeline editing
    // ─────────────────────────────────────────────────────────────────────────

    fun addMusicEvent() {
        val position     = _audioPositionMs.value.toLong()
        val intensityMap = channels.mapIndexed { i, ch -> ch to _composerIntensities.value[i] }
            .filter { it.second > 0 }.toMap()
        val finalMap = if (intensityMap.isEmpty()) mapOf(channels[0] to 2) else intensityMap

        val newEvent = MusicStudioEvent(
            id = nextEventId(), projectId = 0,
            timestampMs = position, channelIntensities = finalMap,
            durationMs = _uiState.value.defaultDurationMs
        )
        _uiState.update { state ->
            state.copy(
                musicEvents = (state.musicEvents + newEvent).sortedBy { it.timestampMs },
                musicProjectSaved = false, selectedEventId = newEvent.id
            )
        }
    }

    fun deleteMusicEvent(event: MusicStudioEvent) {
        _uiState.update { state -> state.copy(musicEvents = state.musicEvents.filter { it.id != event.id }) }
    }

    fun updateEventPosition(event: MusicStudioEvent, newTimeMs: Long) {
        _uiState.update { state ->
            val max = (state.audioDurationMs - event.durationMs).coerceAtLeast(0).toLong()
            state.copy(musicEvents = state.musicEvents.map {
                if (it.id == event.id) it.copy(timestampMs = newTimeMs.coerceIn(0L, max)) else it
            }.sortedBy { it.timestampMs })
        }
    }

    fun updateEventStartAndDuration(event: MusicStudioEvent, newTimestampMs: Long, newDurationMs: Int) {
        _uiState.update { state ->
            val maxTs  = (state.audioDurationMs - 50).toLong().coerceAtLeast(0L)
            val finalTs  = newTimestampMs.coerceIn(0L, maxTs)
            val maxDur = (state.audioDurationMs - finalTs).toInt().coerceAtLeast(50)
            val finalDur = newDurationMs.coerceIn(50, maxDur)
            state.copy(musicEvents = state.musicEvents.map {
                if (it.id == event.id) it.copy(timestampMs = finalTs, durationMs = finalDur) else it
            }.sortedBy { it.timestampMs })
        }
    }

    fun resetProject() {
        stopMusicStudio()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseVisualizer()
        _uiState.value = MusicStudioUiState()
        _audioPositionMs.value = 0
        _visualizerData.value = List(16) { 0f }
    }

    fun clearAllMusicEvents() { _uiState.update { it.copy(musicEvents = emptyList()) } }

    // ─────────────────────────────────────────────────────────────────────────
    // Live visualizer (while playing — drives the frequency bar in the UI)
    // ─────────────────────────────────────────────────────────────────────────

    private fun analyzeFft(fft: ByteArray) {
        val n         = fft.size
        val barsCount = 16
        // Android Visualizer FFT layout:
        //   fft[0]      = DC real (no imaginary)
        //   fft[1]      = Nyquist real (no imaginary)  
        //   fft[2k], fft[2k+1] = real, imag of bin k  (k = 1 .. n/2-1)
        // Bytes are SIGNED (-128..127). Cast to unsigned before magnitude.
        val totalBins = (n / 2).coerceAtLeast(1)
        val groupSize = (totalBins / barsCount).coerceAtLeast(1)

        val magnitudes = (0 until barsCount).map { bar ->
            var maxMag = 0f
            for (j in 0 until groupSize) {
                val bin = bar * groupSize + j + 1   // skip DC bin 0
                val re  = bin * 2
                val im  = re + 1
                if (im < n) {
                    // toInt() and 0xFF converts signed byte → unsigned 0..255
                    val real = (fft[re].toInt() and 0xFF).toFloat()
                    val imag = (fft[im].toInt() and 0xFF).toFloat()
                    val mag  = hypot(real, imag)
                    if (mag > maxMag) maxMag = mag
                }
            }
            // Log-scale so quiet passages stay visible with higher contrast
            val logMag = if (maxMag > 1f)
                (Math.log10(maxMag.toDouble()) / Math.log10(362.0) * 260f).toFloat()
            else 0f
            logMag.coerceIn(0f, 255f)
        }
        _visualizerData.value = magnitudes

        val state = _uiState.value
        if (!state.isAudioPlaying || state.musicEvents.isNotEmpty() || state.selectedAlgorithm == BeatAlgorithm.MANUAL_EDIT) return

        val now = System.currentTimeMillis()
        // Remap frequency indices for 0-255 magnitudes (threshold now ~30 instead of 10)
        val freqIndices = listOf(15, 12, 9, 6, 3, 1, 0)
        val detected = MutableList(7) { 0 }
        var anyBeat = false

        freqIndices.forEachIndexed { i, freqIdx ->
            val energy  = magnitudes[freqIdx]
            val history = energyHistory[i]
            val avg     = if (history.isEmpty()) 0f else history.average().toFloat()
            if (energy > avg * (if (i < 6) SENSITIVITY else 1.4f) && energy > 30f) {
                detected[i] = when {
                    energy > 180f -> 3
                    energy > 100f -> 2
                    else          -> 1
                }
                anyBeat = true
            }
            history.add(energy)
            if (history.size > HISTORY_SIZE) history.removeAt(0)
        }

        if (anyBeat && now - lastFftPulseTime > MIN_PULSE_INTERVAL) {
            _liveGlyphIntensities.value = detected
            glyphController.applyGlyphStateWithIntensities(
                channels.mapIndexed { i, ch -> ch to detected[i] }.toMap(), 100
            )
            lastFftPulseTime = now
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save / Load
    // ─────────────────────────────────────────────────────────────────────────

    fun saveMusicProject(projectName: String) {
        val state = _uiState.value
        val uri = state.audioUri ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "MusicStudio").apply { mkdirs() }
            val file = File(dir, "audio_${System.currentTimeMillis()}.mp3")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { ins ->
                    FileOutputStream(file).use { out -> ins.copyTo(out) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            repository.saveMusicProject(
                MusicStudioProject(0, projectName.ifBlank { state.audioName ?: "Untitled" }, file.absolutePath, null),
                state.musicEvents
            )
            _uiState.update { it.copy(isSaving = false, musicProjectSaved = true, showSaveSuccess = true) }
            delay(3000)
            _uiState.update { it.copy(showSaveSuccess = false) }
        }
    }

    fun playMusicProject(project: MusicProjectWithEvents) {
        stopMusicStudio(); mediaPlayer?.release(); releaseVisualizer()
        val f = File(project.project.localAudioPath)
        if (!f.exists()) {
            Log.e("MusicStudioViewModel", "Audio file not found at ${project.project.localAudioPath}")
            return
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnPreparedListener { mp ->
                    _uiState.update {
                        it.copy(
                            audioUri = f.absolutePath.toUri(),
                            audioName = project.project.name,
                            audioDurationMs = mp.duration,
                            isAudioPlaying = true,
                            musicEvents = project.events,
                            activeProjectId = project.project.id
                        )
                    }
                    _audioPositionMs.value = 0
                    start(); setupVisualizer(); startMusicStudio()
                }
                prepareAsync()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun relinkAudioAndPlay(project: MusicProjectWithEvents, audioUri: Uri) {
        viewModelScope.launch {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "MusicStudio").apply { mkdirs() }
            val file = File(dir, "audio_${System.currentTimeMillis()}.mp3")
            try {
                getApplication<Application>().contentResolver.openInputStream(audioUri)?.use { ins ->
                    FileOutputStream(file).use { out -> ins.copyTo(out) }
                }
            } catch (e: Exception) {
                Log.e("MusicStudioViewModel", "Failed to copy relinked audio", e)
                return@launch
            }

            val updatedProject = project.project.copy(localAudioPath = file.absolutePath)
            repository.updateMusicProject(updatedProject)

            // Trigger playback with updated path
            playMusicProject(project.copy(project = updatedProject))
        }
    }

    fun deleteMusicProject(project: MusicStudioProject) {
        viewModelScope.launch {
            if (_uiState.value.activeProjectId == project.id) { mediaPlayer?.stop(); stopMusicStudio() }
            try { File(project.localAudioPath).delete() } catch (e: Exception) {}
            repository.deleteMusicProject(project)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visualizer lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    fun retryVisualizerSetup() { if (_uiState.value.isAudioPlaying && visualizer == null) setupVisualizer() }

    private fun releaseVisualizer() {
        try { visualizer?.enabled = false; visualizer?.release(); visualizer = null } catch (e: Exception) {}
    }

    private fun setupVisualizer() {
        val sid = mediaPlayer?.audioSessionId ?: return
        if (sid == 0) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                getApplication(), android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
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
        releaseVisualizer()
        mediaPlayer?.release()
        glyphController.turnOffGlyphs()
    }
}