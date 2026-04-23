package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.smaarig.glyphbarcomposer.repository.GlyphRepository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smaarig.glyphbarcomposer.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LibraryViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {
    val allPlaylists: Flow<List<PlaylistWithSteps>> = repository.allPlaylists
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = repository.allMusicProjects

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun deleteMusicProject(project: MusicStudioProject) {
        viewModelScope.launch {
            repository.deleteMusicProject(project)
        }
    }

    fun exportPlaylist(context: Context, item: PlaylistWithSteps) {
        try {
            val json = JSONObject().apply {
                put("type", "sequence")
                put("name", item.playlist.name)
                val steps = JSONArray()
                item.steps.forEach { step ->
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exportMusicProject(context: Context, item: MusicProjectWithEvents) {
        try {
            val json = JSONObject().apply {
                put("type", "studio")
                put("name", item.project.name)
                val events = JSONArray()
                item.events.forEach { event ->
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
            shareJsonFile(context, "${item.project.name}.gstudio", json.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun shareJsonFile(context: Context, fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Glyph Item"))
    }

    fun importItem(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                val json = JSONObject(content)
                val type = json.getString("type")
                val name = json.getString("name")

                if (type == "sequence") {
                    val stepsJson = json.getJSONArray("steps")
                    val steps = mutableListOf<SequenceStep>()
                    for (i in 0 until stepsJson.length()) {
                        val s = stepsJson.getJSONObject(i)
                        val channels = mutableMapOf<Int, Int>()
                        val chJson = s.getJSONObject("channels")
                        chJson.keys().forEach { ch -> channels[ch.toInt()] = chJson.getInt(ch) }
                        steps.add(SequenceStep(0, 0, i, channels, s.getInt("duration")))
                    }
                    repository.savePlaylist(Playlist(name = "$name (Imported)"), steps)
                } else if (type == "studio") {
                    val eventsJson = json.getJSONArray("events")
                    val events = mutableListOf<MusicStudioEvent>()
                    for (i in 0 until eventsJson.length()) {
                        val e = eventsJson.getJSONObject(i)
                        val channels = mutableMapOf<Int, Int>()
                        val chJson = e.getJSONObject("channels")
                        chJson.keys().forEach { ch -> channels[ch.toInt()] = chJson.getInt(ch) }
                        events.add(MusicStudioEvent(0, 0, e.getLong("timestamp"), channels, e.getInt("duration")))
                    }
                    repository.saveMusicProject(MusicStudioProject(0, "$name (Imported)", "", null), events)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
