package com.smaarig.glyphbarcomposer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistWithSteps>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert
    suspend fun insertSteps(steps: List<SequenceStep>)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventBinding(binding: EventBinding)

    @Transaction
    @Query("SELECT * FROM event_bindings")
    fun getAllEventBindings(): Flow<List<EventBindingWithPlaylist>>

    @Delete
    suspend fun deleteEventBinding(binding: EventBinding)

    // Music Studio
    @Insert
    suspend fun insertMusicProject(project: MusicStudioProject): Long

    @Insert
    suspend fun insertMusicEvents(events: List<MusicStudioEvent>)

    @Transaction
    @Query("SELECT * FROM music_studio_projects")
    fun getAllMusicProjects(): Flow<List<MusicProjectWithEvents>>

    @Delete
    suspend fun deleteMusicProject(project: MusicStudioProject)

    @Update
    suspend fun updateMusicProject(project: MusicStudioProject)

    // Contact Ringtone Bindings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactBinding(binding: ContactBinding)

    @Transaction
    @Query("SELECT * FROM contact_bindings")
    fun getAllContactBindings(): Flow<List<ContactBindingWithPlaylist>>

    @Transaction
    @Query("SELECT * FROM contact_bindings")
    suspend fun getContactBindingsList(): List<ContactBindingWithPlaylist>

    @Delete
    suspend fun deleteContactBinding(binding: ContactBinding)

    @Transaction
    @Query("SELECT * FROM contact_bindings WHERE contactId = :contactId LIMIT 1")
    suspend fun getContactBinding(contactId: String): ContactBindingWithPlaylist?

    // Notification Hooks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationHook(hook: NotificationHook)

    @Transaction
    @Query("SELECT * FROM notification_hooks")
    fun getAllNotificationHooks(): Flow<List<NotificationHookWithPlaylist>>

    @Transaction
    @Query("SELECT * FROM notification_hooks")
    suspend fun getNotificationHooksList(): List<NotificationHookWithPlaylist>

    @Delete
    suspend fun deleteNotificationHook(hook: NotificationHook)

    @Transaction
    @Query("SELECT * FROM notification_hooks WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getHooksForPackage(packageName: String): List<NotificationHookWithPlaylist>

    @Query("SELECT * FROM notification_hooks WHERE id = :hookId LIMIT 1")
    suspend fun getNotificationHookById(hookId: Long): NotificationHook?

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistWithSteps(playlistId: Long): PlaylistWithSteps?
}
