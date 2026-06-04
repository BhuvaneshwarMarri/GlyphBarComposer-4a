package com.smaarig.glyphbarcomposer.repository

import android.util.Log
import com.smaarig.glyphbarcomposer.data.ContactBinding
import com.smaarig.glyphbarcomposer.data.ContactBindingWithPlaylist
import com.smaarig.glyphbarcomposer.data.EventBinding
import com.smaarig.glyphbarcomposer.data.EventBindingWithPlaylist
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.data.NotificationHook
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistDao
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import kotlinx.coroutines.flow.Flow

class GlyphRepository(private val playlistDao: PlaylistDao) {
    private val TAG = "GlyphRepository"

    val allPlaylists: Flow<List<PlaylistWithSteps>> = playlistDao.getAllPlaylists()
    val allMusicProjects: Flow<List<MusicProjectWithEvents>> = playlistDao.getAllMusicProjects()
    val allEventBindings: Flow<List<EventBindingWithPlaylist>> = playlistDao.getAllEventBindings()
    val allContactBindings: Flow<List<ContactBindingWithPlaylist>> =
        playlistDao.getAllContactBindings()
    val allNotificationHooks: Flow<List<NotificationHookWithPlaylist>> =
        playlistDao.getAllNotificationHooks()

    suspend fun savePlaylist(playlist: Playlist, steps: List<SequenceStep>) {
        Log.d(TAG, "Saving playlist: ${playlist.name} with ${steps.size} steps")
        if (playlist.id != 0L) {
            playlistDao.replacePlaylistSteps(playlist, steps.map { it.copy(playlistId = playlist.id) })
        } else {
            val newId = playlistDao.insertPlaylist(playlist)
            playlistDao.insertSteps(steps.map { it.copy(playlistId = newId) })
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        Log.d(TAG, "Deleting playlist: ${playlist.name}")
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun saveMusicProject(project: MusicStudioProject, events: List<MusicStudioEvent>) {
        Log.d(TAG, "Saving music project: ${project.name} with ${events.size} events")
        if (project.id != 0L) {
            val eventsWithId = events.map { it.copy(projectId = project.id) }
            playlistDao.replaceMusicProjectEvents(project, eventsWithId)
        } else {
            val newId = playlistDao.insertMusicProject(project)
            playlistDao.insertMusicEvents(events.map { it.copy(projectId = newId) })
        }
    }

    suspend fun deleteMusicProject(project: MusicStudioProject) {
        Log.d(TAG, "Deleting music project: ${project.name}")
        playlistDao.deleteMusicProject(project)
    }

    suspend fun updateMusicProject(project: MusicStudioProject) {
        Log.d(TAG, "Updating music project: ${project.name}")
        playlistDao.updateMusicProject(project)
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

    suspend fun saveNotificationHook(hook: NotificationHook) {
        Log.d(TAG, "Saving notification hook for package: ${hook.packageName}")
        playlistDao.insertNotificationHook(hook)
    }

    suspend fun deleteNotificationHook(hook: NotificationHook) {
        Log.d(TAG, "Deleting notification hook for package: ${hook.packageName}")
        playlistDao.deleteNotificationHook(hook)
    }

    suspend fun getHooksForPackage(packageName: String): List<NotificationHookWithPlaylist> {
        return playlistDao.getHooksForPackage(packageName)
    }

    suspend fun getPlaylistWithSteps(playlistId: Long): PlaylistWithSteps? {
        return playlistDao.getPlaylistWithSteps(playlistId)
    }

    suspend fun getNotificationHookSync(hookId: Long): NotificationHook? {
        return playlistDao.getNotificationHookById(hookId)
    }
}
