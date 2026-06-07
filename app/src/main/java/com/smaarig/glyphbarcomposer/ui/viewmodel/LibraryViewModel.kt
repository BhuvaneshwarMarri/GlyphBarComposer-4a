package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.utils.GlyphSequenceExporter
import com.smaarig.glyphbarcomposer.utils.GlyphSequenceImporter
import com.smaarig.glyphbarcomposer.utils.ZipUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LibraryViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    val allPlaylists: Flow<List<PlaylistWithSteps>> = repository.allPlaylists
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = repository.allMusicProjects

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredPlaylists: StateFlow<List<PlaylistWithSteps>> = combine(
        repository.allPlaylists,
        _searchQuery
    ) { playlists, query ->
        if (query.isBlank()) {
            playlists.sortedBy { it.playlist.name.lowercase() }
        } else {
            playlists.filter { it.playlist.name.contains(query, ignoreCase = true) }
                .sortedBy { it.playlist.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMusicProjects: StateFlow<List<MusicProjectWithEvents>> = combine(
        repository.allMusicProjects,
        _searchQuery
    ) { projects, query ->
        if (query.isBlank()) {
            projects.sortedBy { it.project.name.lowercase() }
        } else {
            projects.filter { it.project.name.contains(query, ignoreCase = true) }
                .sortedBy { it.project.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class PendingImport(
        val name: String,
        val steps: List<SequenceStep>
    )

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    fun confirmImport(name: String) {
        val pending = _pendingImport.value ?: return
        viewModelScope.launch {
            repository.savePlaylist(Playlist(name = name), pending.steps)
            _pendingImport.value = null
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun deleteMusicProject(project: MusicStudioProject) {
        viewModelScope.launch { repository.deleteMusicProject(project) }
    }

    // ── Export / Share ───────────────────────────────────────────────────────

    fun exportPlaylist(context: Context, item: PlaylistWithSteps) {
        try {
            val json = JSONObject().apply {
                put("type", "sequence")
                put("name", item.playlist.name)
                val steps = JSONArray()
                // Sort by stepIndex so the order is preserved after sharing
                item.steps.sortedBy { it.stepIndex }.forEach { step ->
                    steps.put(JSONObject().apply {
                        put("duration", step.durationMs)
                        val intensities = JSONObject()
                        step.channelIntensities.forEach { (ch, intensity) ->
                            intensities.put(ch.toString(), intensity)
                        }
                        put("channels", intensities)
                    })
                }
                put("steps", steps)
            }
            shareJsonFile(context, "${item.playlist.name}.glyph", json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "exportPlaylist failed", e)
        }
    }

    fun exportPlaylistAsCsv(context: Context, item: PlaylistWithSteps) {
        try {
            val csv = GlyphSequenceExporter.exportToCsv(item.steps)
            shareTextFile(context, "${item.playlist.name}.csv", csv, "text/csv")
        } catch (e: Exception) {
            Log.e(TAG, "exportPlaylistAsCsv failed", e)
        }
    }

    fun exportPlaylistAsJson(context: Context, item: PlaylistWithSteps) {
        try {
            val json = GlyphSequenceExporter.exportToJson(item.playlist.name, item.steps)
            shareTextFile(context, "${item.playlist.name}.json", json, "application/json")
        } catch (e: Exception) {
            Log.e(TAG, "exportPlaylistAsJson failed", e)
        }
    }

    fun exportMusicProject(context: Context, item: MusicProjectWithEvents) {
        try {
            val json = JSONObject().apply {
                put("type", "studio")
                put("name", item.project.name)
                val events = JSONArray()
                item.events.sortedBy { it.timestampMs }.forEach { event ->
                    events.put(JSONObject().apply {
                        put("timestamp", event.timestampMs)
                        put("duration", event.durationMs)
                        val intensities = JSONObject()
                        event.channelIntensities.forEach { (ch, intensity) ->
                            intensities.put(ch.toString(), intensity)
                        }
                        put("channels", intensities)
                    })
                }
                put("events", events)
            }

            val audioFile = File(item.project.localAudioPath)
            if (audioFile.exists()) {
                val jsonFile = File(context.cacheDir, "project.json")
                jsonFile.writeText(json.toString(), Charsets.UTF_8)

                val zipFile = File(context.cacheDir, "${item.project.name}.gstudio")
                ZipUtils.zipFiles(
                    zipFile, mapOf(
                        "project.json" to jsonFile,
                        "audio.mp3" to audioFile
                    )
                )

                shareFile(context, zipFile.name, zipFile, "application/zip")
            } else {
                shareJsonFile(context, "${item.project.name}.gstudio", json.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "exportMusicProject failed", e)
        }
    }

    private fun shareFile(context: Context, fileName: String, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Glyph Item"))
    }

    private fun shareJsonFile(context: Context, fileName: String, content: String) {
        shareTextFile(context, fileName, content, "application/json")
    }

    private fun shareTextFile(context: Context, fileName: String, content: String, mimeType: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Glyph Item"))
    }

    // ── Import ───────────────────────────────────────────────────────────────

    fun importItem(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                if (ZipUtils.isZipFile(context, uri)) {
                    importZipBundle(context, uri)
                    return@launch
                }

                val content = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }

                if (content.isNullOrBlank()) {
                    _importError.value = "File is empty"
                    return@launch
                }

                // 1. Try CSV
                if (!content.trimStart().startsWith("{") && !content.trimStart().startsWith("[")) {
                    try {
                        val steps = GlyphSequenceImporter.importFromCsv(content)
                        _pendingImport.value = PendingImport("Imported CSV", steps)
                        return@launch
                    } catch (e: Exception) {
                        Log.d(TAG, "CSV import failed: ${e.message}")
                    }
                }

                // 2. Try Custom JSON
                try {
                    val steps = GlyphSequenceImporter.importFromJson(content)
                    _pendingImport.value = PendingImport("Imported JSON", steps)
                    return@launch
                } catch (e: Exception) {
                    Log.d(TAG, "Custom JSON import failed: ${e.message}")
                }

                // 3. Try Legacy JSON
                try {
                    val json = JSONObject(content)
                    val type = json.optString("type")
                    val name = json.optString("name", "Imported")

                    when (type) {
                        "sequence" -> {
                            val steps = parseLegacySequence(json)
                            _pendingImport.value = PendingImport(name, steps)
                        }
                        "studio" -> importStudio(json, name, null)
                        else -> _importError.value = "Unknown file type: $type"
                    }
                } catch (e: Exception) {
                    _importError.value = "Invalid file format"
                }

            } catch (e: Exception) {
                Log.e(TAG, "importItem failed", e)
                _importError.value = "Import failed: ${e.message}"
            }
        }
    }

    private suspend fun importZipBundle(context: Context, uri: android.net.Uri) {
        val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val extracted = ZipUtils.unzipToDirectory(context, uri, tempDir)

            val jsonFile = extracted.find { it.name == "project.json" }
                ?: throw IllegalArgumentException("No project.json in archive")
            val audioFile = extracted.find { it.name == "audio.mp3" }

            val json = JSONObject(jsonFile.readText(Charsets.UTF_8))
            val name = json.optString("name", "Imported Project")

            var finalAudioPath: String? = null
            if (audioFile != null && audioFile.exists()) {
                val dest = File(
                    context.getExternalFilesDir(null),
                    "MusicStudio/audio_${System.currentTimeMillis()}.mp3"
                ).also { it.parentFile?.mkdirs() }
                audioFile.copyTo(dest, overwrite = true)
                finalAudioPath = dest.absolutePath
            }

            importStudio(json, name, finalAudioPath)

        } catch (e: Exception) {
            Log.e(TAG, "importZipBundle failed", e)
            _importError.value = "ZIP Import failed: ${e.message}"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun parseLegacySequence(json: JSONObject): List<SequenceStep> {
        val stepsJson = json.getJSONArray("steps")
        val steps = mutableListOf<SequenceStep>()

        for (i in 0 until stepsJson.length()) {
            val s = stepsJson.getJSONObject(i)
            val channels = mutableMapOf<Int, Int>()
            val chJson = s.getJSONObject("channels")
            chJson.keys().forEach { ch -> channels[ch.toInt()] = chJson.getInt(ch) }
            steps.add(
                SequenceStep(
                    stepId = 0,
                    playlistId = 0,
                    stepIndex = i,
                    channelIntensities = channels,
                    durationMs = s.getInt("duration")
                )
            )
        }
        return steps
    }

    private suspend fun importStudio(json: JSONObject, name: String, audioPath: String?) {
        val eventsJson = json.getJSONArray("events")
        val events = mutableListOf<MusicStudioEvent>()

        for (i in 0 until eventsJson.length()) {
            val e = eventsJson.getJSONObject(i)
            val channels = mutableMapOf<Int, Int>()
            val chJson = e.getJSONObject("channels")
            chJson.keys().forEach { ch -> channels[ch.toInt()] = chJson.getInt(ch) }
            events.add(
                MusicStudioEvent(
                    id = 0,
                    projectId = 0,
                    timestampMs = e.getLong("timestamp"),
                    channelIntensities = channels,
                    durationMs = e.getInt("duration")
                )
            )
        }

        val project = MusicStudioProject(
            id = 0,
            name = "$name (Imported)",
            localAudioPath = audioPath ?: "",
            localGlyphPath = null
        )

        Log.d(TAG, "importStudio: saving ${events.size} events for '${project.name}'")
        repository.saveMusicProject(project, events)
    }
}
