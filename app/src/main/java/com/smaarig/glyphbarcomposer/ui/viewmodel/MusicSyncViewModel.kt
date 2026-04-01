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

data class MusicSyncUiState(
    val audioUri: Uri? = null,
    val audioName: String? = null,
    val audioDurationMs: Int = 0,
    val isAudioPlaying: Boolean = false,
    val musicEvents: List<MusicSyncEvent> = emptyList(),
    val activeProjectId: Long? = null,
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

    // Decoupled flows for high-frequency data to prevent full-screen recomposition
    private val _visualizerData = MutableStateFlow(List(16) { 0f })
    val visualizerData: StateFlow<List<Float>> = _visualizerData.asStateFlow()

    private val _audioPositionMs = MutableStateFlow(0)
    val audioPositionMs: StateFlow<Int> = _audioPositionMs.asStateFlow()

    private val _glyphIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0))
    val glyphIntensities: StateFlow<List<Int>> = _glyphIntensities.asStateFlow()

    private var musicSyncJob: Job? = null

    private val channels = listOf(
        Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6
    )

    fun onIntensityChange(index: Int, newIntensity: Int) {
        if (_uiState.value.isAudioPlaying) return
        _glyphIntensities.update { it.toMutableList().apply { this[index] = newIntensity } }
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
                audioUri = uri, audioName = name, audioDurationMs = duration, 
                isAudioPlaying = false, musicEvents = emptyList(), isAnalyzing = true, 
                isAnalysisComplete = false, isPlaybackStarted = false, musicProjectSaved = false
            ) }
            _audioPositionMs.value = 0
        }
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
                generatedEvents.add(MusicSyncEvent(projectId = 0, timestampMs = currentMs, channelIntensities = intensities, durationMs = 100))
                currentMs += (400 + random.nextInt(400))
                if (currentMs % 5000 < 800) delay(1)
            }
            delay(1500)
            _uiState.update { it.copy(musicEvents = generatedEvents, isAnalysisComplete = true) }
            delay(2000)
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }


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
        
        // Update visualizer data through decoupled flow
        _visualizerData.value = magnitudes
        
        // Map 6 dots to specific frequency bands:
        // A1-A2 (High/Treble), A3-A4 (Mid), A5-A6 (Low/Bass)
        val dotIndices = listOf(14, 11, 8, 5, 2, 1)
        val dotIntensities = dotIndices.map { idx ->
            val mag = magnitudes[idx]
            if (mag > 80) 3 else if (mag > 40) 2 else if (mag > 15) 1 else 0
        }

        if (_uiState.value.isAudioPlaying || !_uiState.value.isAudioPlaying) {
            _glyphIntensities.value = dotIntensities
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
            startMusicSync()
        }
    }

    fun retryVisualizerSetup() {
        if (_uiState.value.isAudioPlaying && visualizer == null) {
            setupVisualizer()
        }
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupVisualizer() {
        val sessionId = mediaPlayer?.audioSessionId ?: return
        if (sessionId == 0) return
        
        // Check for permission before creating visualizer
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w("MusicSyncVM", "Cannot setup visualizer: Permission denied")
            return
        }

        try {
            releaseVisualizer()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null || !_uiState.value.isAudioPlaying) return
                        analyzeFft(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
            android.util.Log.d("MusicSyncVM", "Visualizer setup successful")
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startAutoProgressOnly() {
        // No longer used, startMusicSync handles progress
    }

    fun deleteMusicEvent(event: MusicSyncEvent) {
        _uiState.update { state -> state.copy(musicEvents = state.musicEvents.filter { it != event }) }
    }

    fun clearAllMusicEvents() {
        _uiState.update { it.copy(musicEvents = emptyList()) }
    }

    fun seekMusic(positionMs: Float) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _audioPositionMs.value = positionMs.toInt()
    }

    fun addMusicEvent() {
        val position = _audioPositionMs.value
        val intensities = getIntensitiesMap()
        if (intensities.values.all { it == 0 }) return
        _uiState.update { it.copy(musicEvents = (it.musicEvents + MusicSyncEvent(projectId = 0, timestampMs = position.toLong(), channelIntensities = intensities, durationMs = 100)).sortedBy { e -> e.timestampMs }, musicProjectSaved = false) }
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
                    isAudioPlaying = true, 
                    musicEvents = project.events, 
                    isAnalyzing = false, 
                    isAnalysisComplete = true, 
                    isPlaybackStarted = true, 
                    activeProjectId = project.project.id
                ) }
                _audioPositionMs.value = 0
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
        musicSyncJob = viewModelScope.launch {
            var lastTriggeredIdx = -1
            while (mediaPlayer?.isPlaying == true) {
                val currentPos = mediaPlayer?.currentPosition ?: 0
                _audioPositionMs.value = currentPos
                val events = _uiState.value.musicEvents
                
                // Efficiency: Only check events around the current position
                events.forEachIndexed { index, event ->
                    if (index > lastTriggeredIdx && currentPos >= event.timestampMs && currentPos < event.timestampMs + 200) {
                        glyphController.applyGlyphStateWithIntensities(event.channelIntensities, event.durationMs)
                        lastTriggeredIdx = index
                        val eventIntensities = channels.map { ch -> event.channelIntensities[ch] ?: 0 }
                        _glyphIntensities.value = eventIntensities
                        
                        // Use a side-effect launch to reset intensities without blocking the loop
                        viewModelScope.launch {
                            delay(event.durationMs.toLong() + 20)
                            // Only reset if the current state still matches the event we triggered
                            if (_glyphIntensities.value == eventIntensities) {
                                _glyphIntensities.value = listOf(0, 0, 0, 0, 0, 0)
                            }
                        }
                    }
                }
                if (events.isNotEmpty() && lastTriggeredIdx >= 0 && currentPos < events[lastTriggeredIdx].timestampMs) lastTriggeredIdx = -1
                delay(16) // ~60fps check rate
            }
        }
    }

    private fun stopMusicSync() {
        musicSyncJob?.cancel()
        glyphController.turnOffGlyphs()
        _uiState.update { it.copy(activeProjectId = null) }
        _glyphIntensities.value = listOf(0, 0, 0, 0, 0, 0)
        _visualizerData.value = List(16) { 0f }
    }

    private fun getIntensitiesMap(): Map<Int, Int> = channels.mapIndexed { i, ch -> ch to _glyphIntensities.value[i] }.toMap()

    override fun onCleared() {
        super.onCleared()
        visualizer?.release()
        mediaPlayer?.release()
        glyphController.deinit()
    }
}
