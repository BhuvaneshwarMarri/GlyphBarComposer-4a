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
}
