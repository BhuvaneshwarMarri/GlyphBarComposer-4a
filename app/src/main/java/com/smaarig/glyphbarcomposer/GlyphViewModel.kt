package com.smaarig.glyphbarcomposer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GlyphViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()

    val allPlaylists: Flow<List<PlaylistWithSteps>> = playlistDao.getAllPlaylists()

    fun savePlaylist(name: String, steps: List<GlyphSequence>) {
        viewModelScope.launch {
            val playlist = Playlist(name = name)
            // insertPlaylist returns the real auto-generated ID
            val playlistId = playlistDao.insertPlaylist(playlist)
            val sequenceSteps = steps.mapIndexed { index, step ->
                SequenceStep(
                    playlistId = playlistId,
                    stepIndex = index,
                    channelIntensities = step.channelIntensities,
                    durationMs = step.durationMs
                )
            }
            playlistDao.insertSteps(sequenceSteps)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }
}