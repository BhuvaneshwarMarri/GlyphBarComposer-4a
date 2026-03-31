package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicSyncProject
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()

    val allPlaylists: Flow<List<PlaylistWithSteps>> = playlistDao.getAllPlaylists()
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = playlistDao.getAllMusicProjects()

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun deleteMusicProject(project: MusicSyncProject) {
        viewModelScope.launch {
            playlistDao.deleteMusicProject(project)
        }
    }
}
