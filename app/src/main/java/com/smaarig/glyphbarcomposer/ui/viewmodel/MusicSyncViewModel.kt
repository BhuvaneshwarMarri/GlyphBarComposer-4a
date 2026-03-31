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
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicSyncEvent
import com.smaarig.glyphbarcomposer.data.MusicSyncProject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

enum class MusicSyncMode {
    MANUAL, AUTO_BEAT
}

data class MusicSyncUiState(
    val glyphIntensities: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val audioUri: Uri? = null,
    val audioName: String? = null,
    val audioDurationMs: Int = 0,
    val audioPositionMs: Int = 0,
    val isAudioPlaying: Boolean = false,
    val musicEvents: List<MusicSyncEvent> = emptyList(),
    val musicSyncMode: MusicSyncMode = MusicSyncMode.MANUAL,
    val activeProjectId: Long? = null,
    val visualizerData: List<Float> = List(16) { 0f },
    val isAnalyzing: Boolean = false,
    val isAnalysisComplete: Boolean = false,
    val isPlaybackStarted: Boolean = false,
    val musicProjectSaved: Boolean = false
)

class MusicSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val glyphController = GlyphController.getInstance(application)
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null

    private val _uiState = MutableStateFlow(MusicSyncUiState())
    val uiState: StateFlow<MusicSyncUiState> = _uiState.asStateFlow()

    private var musicSyncJob: Job? = null
    private var audioProgressJob: Job? = null

    private val channels = listOf(
        Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6
    )

    fun onIntensityChange(index: Int, newIntensity: Int) {
        if (_uiState.value.isAudioPlaying) return
        _uiState.update { it.copy(glyphIntensities = it.glyphIntensities.toMutableList().apply { this[index] = newIntensity }) }
        glyphController.applyGlyphStateWithIntensities(getIntensitiesMap(), 2000)
    }

    fun loadSong(uri: Uri, name: String) {
        if (_uiState.value.isAudioPlaying) toggleMusicPlayback()
        mediaPlayer?.release()
        releaseVisualizer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), uri)
            prepare()
            _uiState.update { it.copy(
                audioUri = uri, audioName = name, audioDurationMs = duration, audioPositionMs = 0, 
                isAudioPlaying = false, musicEvents = emptyList(), isAnalyzing = true, 
                isAnalysisComplete = false, isPlaybackStarted = false, musicProjectSaved = false
            ) }
        }
        bassHistory.clear()
        startAudioAnalysis()
    }

    private fun startAudioAnalysis() {
        val duration = _uiState.value.audioDurationMs
        if (duration <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(musicEvents = emptyList()) }
            val generatedEvents = mutableListOf<MusicSyncEvent>()
            val random = java.util.Random()
            var currentMs = 200L
            while (currentMs < duration - 200) {
                val activeCount = 1 + random.nextInt(3)
                val activeIndices = (0..5).shuffled().take(activeCount)
                val intensities = activeIndices.associate { channels[it] to (2 + random.nextInt(2)) }
                generatedEvents.add(MusicSyncEvent(projectId = 0, timestampMs = currentMs, channelIntensities = intensities, durationMs = 150))
                currentMs += (400 + random.nextInt(400))
                if (currentMs % 5000 < 800) delay(1)
            }
            delay(1500)
            _uiState.update { it.copy(musicEvents = generatedEvents, isAnalyzing = false, isAnalysisComplete = true) }
        }
    }

    private fun setupVisualizer() {
        val sessionId = mediaPlayer?.audioSessionId ?: return
        if (sessionId == 0) return
        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null || !_uiState.value.isAudioPlaying) return
                        analyzeFft(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private val bassHistory = mutableListOf<Float>()
    private var lastBeatTime = 0L

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
        
        // Map 6 dots to specific frequency bands:
        // A1-A2 (High/Treble), A3-A4 (Mid), A5-A6 (Low/Bass)
        val dotIndices = listOf(14, 11, 8, 5, 2, 1)
        val dotIntensities = dotIndices.map { idx ->
            val mag = magnitudes[idx]
            if (mag > 80) 3 else if (mag > 40) 2 else if (mag > 15) 1 else 0
        }

        _uiState.update { it.copy(
            visualizerData = magnitudes,
            glyphIntensities = if (it.musicSyncMode == MusicSyncMode.AUTO_BEAT || !it.isAudioPlaying) dotIntensities else it.glyphIntensities
        ) }

        // Sync physical glyphs in AUTO_BEAT mode
        if (_uiState.value.isAudioPlaying && _uiState.value.musicSyncMode == MusicSyncMode.AUTO_BEAT) {
            val intensitiesMap = channels.mapIndexed { i, ch -> ch to dotIntensities[i] }.toMap()
            
            // Apply current frequency analysis to glyphs (all 6 channels)
            glyphController?.applyGlyphStateWithIntensities(intensitiesMap, 100)
            
            // Extra beat detection for history/averaging
            val currentBassEnergy = magnitudes.slice(1..3).sum()
            val avgBass = if (bassHistory.isEmpty()) 0f else bassHistory.average().toFloat()
            val threshold = 1.4f
            val currentTime = System.currentTimeMillis()
            if (currentBassEnergy > (avgBass * threshold) && currentTime - lastBeatTime > 300) {
                lastBeatTime = currentTime
            }
            bassHistory.add(currentBassEnergy)
            if (bassHistory.size > 50) bassHistory.removeAt(0)
        }
    }

    fun toggleMusicPlayback() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            releaseVisualizer()
            _uiState.update { it.copy(isAudioPlaying = false) }
            stopMusicSync()
        } else {
            _uiState.update { it.copy(isAudioPlaying = true, isPlaybackStarted = true) }
            mp.start()
            setupVisualizer()
            visualizer?.enabled = true
            if (_uiState.value.musicSyncMode == MusicSyncMode.MANUAL) startMusicSync() else startAutoProgressOnly()
        }
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startAutoProgressOnly() {
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                _uiState.update { it.copy(audioPositionMs = mediaPlayer?.currentPosition ?: 0) }
                delay(50)
            }
        }
    }

    fun setMusicSyncMode(mode: MusicSyncMode) {
        val wasPlaying = _uiState.value.isAudioPlaying
        _uiState.update { it.copy(musicSyncMode = mode) }
        
        if (wasPlaying) {
            // Restart the appropriate sync job without stopping audio
            musicSyncJob?.cancel()
            if (mode == MusicSyncMode.MANUAL) {
                startMusicSync()
            } else {
                startAutoProgressOnly()
                // Ensure visualizer is active for AUTO_BEAT mode
                if (visualizer == null) {
                    setupVisualizer()
                    visualizer?.enabled = true
                }
            }
        }
    }

    fun deleteMusicEvent(event: MusicSyncEvent) {
        _uiState.update { state -> state.copy(musicEvents = state.musicEvents.filter { it != event }) }
    }

    fun clearAllMusicEvents() {
        _uiState.update { it.copy(musicEvents = emptyList()) }
    }

    fun seekMusic(positionMs: Float) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _uiState.update { it.copy(audioPositionMs = positionMs.toInt()) }
        bassHistory.clear()
        lastBeatTime = 0
    }

    fun addMusicEvent() {
        val position = _uiState.value.audioPositionMs
        val intensities = getIntensitiesMap()
        if (intensities.values.all { it == 0 }) return
        _uiState.update { it.copy(musicEvents = (it.musicEvents + MusicSyncEvent(projectId = 0, timestampMs = position.toLong(), channelIntensities = intensities)).sortedBy { e -> e.timestampMs }, musicProjectSaved = false) }
    }

    fun saveMusicProject() {
        val state = _uiState.value
        val events = state.musicEvents
        val sourceUri = state.audioUri ?: return
        val name = state.audioName ?: "Untitled Project"
        if (events.isEmpty()) return
        viewModelScope.launch {
            val musicSyncDir = File(getApplication<Application>().getExternalFilesDir(null), "MusicSync").apply { mkdirs() }
            val localAudioFile = File(musicSyncDir, "audio_${System.currentTimeMillis()}.mp3")
            try {
                getApplication<Application>().contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(localAudioFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) { return@launch }
            val localGlyphFile = File(musicSyncDir, "glyphs_${System.currentTimeMillis()}.gsync")
            try {
                val json = JSONArray().apply {
                    events.forEach { event ->
                        put(JSONObject().apply {
                            put("timestampMs", event.timestampMs)
                            put("durationMs", event.durationMs)
                            val intensities = JSONObject()
                            event.channelIntensities.forEach { (ch, intensity) -> intensities.put(ch.toString(), intensity) }
                            put("intensities", intensities)
                        })
                    }
                }
                localGlyphFile.writeText(json.toString())
            } catch (e: Exception) {}
            val project = MusicSyncProject(name = name, localAudioPath = localAudioFile.absolutePath, localGlyphPath = localGlyphFile.absolutePath)
            val projectId = playlistDao.insertMusicProject(project)
            playlistDao.insertMusicEvents(events.map { it.copy(projectId = projectId) })
            _uiState.update { it.copy(musicProjectSaved = true) }
        }
    }

    fun playMusicProject(project: MusicProjectWithEvents) {
        val state = _uiState.value
        if (state.activeProjectId == project.project.id && mediaPlayer != null) {
            toggleMusicPlayback()
            return
        }
        
        stopMusicSync()
        mediaPlayer?.release()
        releaseVisualizer()
        
        val audioFile = File(project.project.localAudioPath)
        if (!audioFile.exists()) return
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                _uiState.update { it.copy(
                    audioUri = audioFile.absolutePath.toUri(), 
                    audioName = project.project.name, 
                    audioDurationMs = duration, 
                    audioPositionMs = 0, 
                    isAudioPlaying = true, 
                    musicEvents = project.events, 
                    isAnalyzing = false, 
                    isAnalysisComplete = true, 
                    isPlaybackStarted = true, 
                    musicSyncMode = MusicSyncMode.MANUAL, 
                    activeProjectId = project.project.id
                ) }
                start()
            }
            setupVisualizer()
            visualizer?.enabled = true
            startMusicSync()
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isAudioPlaying = false, isAnalyzing = false) }
        }
    }

    fun deleteMusicProject(project: MusicSyncProject) {
        viewModelScope.launch {
            if (_uiState.value.activeProjectId == project.id) {
                mediaPlayer?.stop()
                stopMusicSync()
            }
            try {
                File(project.localAudioPath).delete()
                project.localGlyphPath?.let { File(it).delete() }
            } catch (e: Exception) {}
            playlistDao.deleteMusicProject(project)
        }
    }

    private fun startMusicSync() {
        musicSyncJob?.cancel()
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                _uiState.update { it.copy(audioPositionMs = mediaPlayer?.currentPosition ?: 0) }
                delay(50)
            }
        }
        musicSyncJob = viewModelScope.launch {
            var lastTriggeredIdx = -1
            while (mediaPlayer?.isPlaying == true) {
                val currentPos = mediaPlayer?.currentPosition ?: 0
                val events = _uiState.value.musicEvents
                events.forEachIndexed { index, event ->
                    if (index > lastTriggeredIdx && currentPos >= event.timestampMs && currentPos < event.timestampMs + 500) {
                        glyphController.applyGlyphStateWithIntensities(event.channelIntensities, event.durationMs)
                        lastTriggeredIdx = index
                        _uiState.update { state -> state.copy(glyphIntensities = channels.map { ch -> event.channelIntensities[ch] ?: 0 }) }
                        viewModelScope.launch {
                            val currentEventIntensities = event.channelIntensities
                            delay(event.durationMs.toLong() + 50)
                            _uiState.update { state ->
                                if (channels.withIndex().all { (i, ch) -> state.glyphIntensities[i] == (currentEventIntensities[ch] ?: 0) }) {
                                    state.copy(glyphIntensities = listOf(0, 0, 0, 0, 0, 0))
                                } else state
                            }
                        }
                    }
                }
                if (events.isNotEmpty() && lastTriggeredIdx >= 0 && currentPos < events[lastTriggeredIdx].timestampMs) lastTriggeredIdx = -1
                delay(10)
            }
        }
    }

    private fun stopMusicSync() {
        musicSyncJob?.cancel()
        audioProgressJob?.cancel()
        glyphController.turnOffGlyphs()
        _uiState.update { it.copy(glyphIntensities = listOf(0, 0, 0, 0, 0, 0), visualizerData = List(16) { 0f }, activeProjectId = null) }
    }

    private fun getIntensitiesMap(): Map<Int, Int> = channels.mapIndexed { i, ch -> ch to _uiState.value.glyphIntensities[i] }.toMap()

    override fun onCleared() {
        super.onCleared()
        visualizer?.release()
        mediaPlayer?.release()
        glyphController.deinit()
    }
}
