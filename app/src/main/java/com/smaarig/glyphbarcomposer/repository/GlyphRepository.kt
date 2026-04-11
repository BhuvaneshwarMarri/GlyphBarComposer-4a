package com.smaarig.glyphbarcomposer.repository

import android.util.Log
import com.smaarig.glyphbarcomposer.data.*
import kotlinx.coroutines.flow.Flow

class GlyphRepository(private val playlistDao: PlaylistDao) {
    private val TAG = "GlyphRepository"

    val allPlaylists: Flow<List<PlaylistWithSteps>> = playlistDao.getAllPlaylists()
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = playlistDao.getAllMusicProjects()
    val allEventBindings: Flow<List<EventBindingWithPlaylist>> = playlistDao.getAllEventBindings()
    val allContactBindings: Flow<List<ContactBindingWithPlaylist>> = playlistDao.getAllContactBindings()

    suspend fun savePlaylist(playlist: Playlist, steps: List<SequenceStep>) {
        Log.d(TAG, "Saving playlist: ${playlist.name} with ${steps.size} steps")
        val id = playlistDao.insertPlaylist(playlist)
        val stepsWithId = steps.map { it.copy(playlistId = id) }
        playlistDao.insertSteps(stepsWithId)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        Log.d(TAG, "Deleting playlist: ${playlist.name}")
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun saveMusicProject(project: MusicSyncProject, events: List<MusicSyncEvent>) {
        Log.d(TAG, "Saving music project: ${project.name} with ${events.size} events")
        val id = playlistDao.insertMusicProject(project)
        val eventsWithId = events.map { it.copy(projectId = id) }
        playlistDao.insertMusicEvents(eventsWithId)
    }

    suspend fun deleteMusicProject(project: MusicSyncProject) {
        Log.d(TAG, "Deleting music project: ${project.name}")
        playlistDao.deleteMusicProject(project)
    }

    suspend fun saveEventBinding(binding: EventBinding) {
        Log.d(TAG, "Saving event binding for ID: ${binding.eventId}")
        playlistDao.insertEventBinding(binding)
    }

    suspend fun deleteEventBinding(binding: EventBinding) {
        Log.d(TAG, "Deleting event binding for ID: ${binding.eventId}")
        playlistDao.deleteEventBinding(binding)
    }

    suspend fun saveContactBinding(binding: ContactBinding) {
        Log.d(TAG, "Saving contact binding for ID: ${binding.contactId}")
        playlistDao.insertContactBinding(binding)
    }

    suspend fun deleteContactBinding(binding: ContactBinding) {
        Log.d(TAG, "Deleting contact binding for ID: ${binding.contactId}")
        playlistDao.deleteContactBinding(binding)
    }

    suspend fun getPlaylistWithSteps(playlistId: Long): PlaylistWithSteps? {
        return playlistDao.getPlaylistWithSteps(playlistId)
    }
}
