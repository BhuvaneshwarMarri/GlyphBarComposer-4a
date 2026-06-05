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
import com.smaarig.glyphbarcomposer.model.ImportError
import com.smaarig.glyphbarcomposer.model.ImportResult
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.utils.CsvSequenceImporter
import com.smaarig.glyphbarcomposer.utils.JsonSequenceImporter
import com.smaarig.glyphbarcomposer.utils.SequenceImportManager
import com.smaarig.glyphbarcomposer.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class ImportState {
    object Idle : ImportState()
    data class Success(val name: String) : ImportState()
    data class Failed(val errors: List<ImportError>) : ImportState()
}

class LibraryViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    val allPlaylists: Flow<List<PlaylistWithSteps>> = repository.allPlaylists
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = repository.allMusicProjects

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun clearImportState() { _importState.value = ImportState.Idle }

    // ── Delete ───────────────────────────────────────────────────────────────

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun deleteMusicProject(project: MusicStudioProject) {
        viewModelScope.launch { repository.deleteMusicProject(project) }
    }

    // ── Export / Share ───────────────────────────────────────────────────────

    fun exportPlaylistAsJson(context: Context, item: PlaylistWithSteps) {
        viewModelScope.launch {
            try {
                val json = JsonSequenceImporter.export(item)
                shareFile(context, "${item.playlist.name}.gbseq.json", json, "application/vnd.glyphbar.sequence+json")
            } catch (e: Exception) {
                Log.e(TAG, "exportPlaylistAsJson failed", e)
            }
        }
    }

    fun exportPlaylistAsCsv(context: Context, item: PlaylistWithSteps) {
        viewModelScope.launch {
            try {
                val csv = CsvSequenceImporter.export(item)
                shareFile(context, "${item.playlist.name}.gbseq.csv", csv, "text/csv")
            } catch (e: Exception) {
                Log.e(TAG, "exportPlaylistAsCsv failed", e)
            }
        }
    }

    private fun shareFile(context: Context, fileName: String, content: String, mimeType: String) {
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
            // Share as application/json — standard MIME that intermediary apps won't
            // re-encode or compress, unlike custom application/vnd.* types.
            shareJsonFile(context, "${item.playlist.name}.glyph", json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "exportPlaylist failed", e)
        }
    }

    fun exportMusicProject(context: Context, item: MusicProjectWithEvents) {
        try {
            val json = JSONObject().apply {
                put("type", "studio")
                put("name", item.project.name)
                val events = JSONArray()
                // Sort by timestamp so playback order is correct after import
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
                // Bundle JSON + Audio into a ZIP
                val jsonFile = File(context.cacheDir, "project.json")
                jsonFile.writeText(json.toString(), Charsets.UTF_8)

                val zipFile = File(context.cacheDir, "${item.project.name}.gstudio")
                ZipUtils.zipFiles(
                    zipFile, mapOf(
                        "project.json" to jsonFile,
                        "audio.mp3" to audioFile // extension doesn't strictly matter inside zip
                    )
                )

                shareFile(context, zipFile.name, zipFile, "application/zip")
            } else {
                // Fallback to legacy JSON-only export
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
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            // application/json is a well-known standard MIME — WhatsApp, Telegram,
            // Gmail will forward it as-is without re-encoding.
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Glyph Item"))
    }

    // ── Import ───────────────────────────────────────────────────────────────

    fun importSequenceFromUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = SequenceImportManager.importFromUri(context, uri)
            withContext(Dispatchers.Main) {
                when (result) {
                    is ImportResult.Success -> {
                        val playlist = Playlist(name = result.name)
                        val steps = result.steps.mapIndexed { index, seq ->
                            SequenceStep(
                                playlistId = 0,
                                stepIndex = index,
                                channelIntensities = seq.channelIntensities,
                                durationMs = seq.durationMs
                            )
                        }
                        repository.savePlaylist(playlist, steps)
                        _importState.value = ImportState.Success(result.name)
                    }
                    is ImportResult.Failure -> {
                        _importState.value = ImportState.Failed(result.errors)
                    }
                    null -> {
                        _importState.value = ImportState.Failed(
                            listOf(ImportError("UNKNOWN_FORMAT", "Unrecognised file format."))
                        )
                    }
                }
            }
        }
    }

    fun importItem(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                // 1. Try new JSON/CSV formats first via SequenceImportManager
                val newResult = withContext(Dispatchers.IO) {
                    SequenceImportManager.importFromUri(context, uri)
                }

                if (newResult != null) {
                    when (newResult) {
                        is ImportResult.Success -> {
                            val playlist = Playlist(name = newResult.name)
                            val steps = newResult.steps.mapIndexed { index, seq ->
                                SequenceStep(
                                    playlistId = 0,
                                    stepIndex = index,
                                    channelIntensities = seq.channelIntensities,
                                    durationMs = seq.durationMs
                                )
                            }
                            repository.savePlaylist(playlist, steps)
                            _importState.value = ImportState.Success(newResult.name)
                        }
                        is ImportResult.Failure -> {
                            _importState.value = ImportState.Failed(newResult.errors)
                        }
                    }
                    return@launch
                }

                // 2. Fall back to existing ZIP bundle import
                if (ZipUtils.isZipFile(context, uri)) {
                    importZipBundle(context, uri)
                    return@launch
                }

                // 3. Fall back to legacy JSON import
                val content = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }

                if (content.isNullOrBlank()) {
                    Log.e(TAG, "importItem: empty content from URI $uri")
                    _importState.value = ImportState.Failed(listOf(ImportError("EMPTY_FILE", "The file is empty.")))
                    return@launch
                }

                val json = JSONObject(content)
                val type = json.optString("type")
                val name = json.optString("name", "Imported")

                Log.d(TAG, "importItem: type=$type name=$name")

                when (type) {
                    "sequence" -> {
                        importSequence(json, name)
                        _importState.value = ImportState.Success(name)
                    }
                    "studio" -> {
                        importStudio(json, name, null)
                        _importState.value = ImportState.Success(name)
                    }
                    else -> {
                        Log.e(TAG, "importItem: unknown type '$type'")
                        _importState.value = ImportState.Failed(listOf(ImportError("UNKNOWN_FORMAT", "Unrecognised file format or data type.")))
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "importItem failed for URI $uri", e)
                _importState.value = ImportState.Failed(listOf(ImportError("IMPORT_ERROR", e.message ?: "Unknown error during import.")))
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
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun importSequence(json: JSONObject, name: String) {
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
                    playlistId = 0,   // replaced by repository after insert
                    stepIndex = i,
                    channelIntensities = channels,
                    durationMs = s.getInt("duration")
                )
            )
        }

        Log.d(TAG, "importSequence: saving ${steps.size} steps for '$name'")
        repository.savePlaylist(Playlist(name = "$name (Imported)"), steps)
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
                    projectId = 0,          // replaced by repository after insert
                    timestampMs = e.getLong("timestamp"),
                    channelIntensities = channels,
                    durationMs = e.getInt("duration")
                )
            )
        }

        // localAudioPath is a non-null String in the Room schema.
        // Imported studio projects have no local audio on the receiving device —
        // use empty string as the sentinel value. localGlyphPath is nullable so null is fine.
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