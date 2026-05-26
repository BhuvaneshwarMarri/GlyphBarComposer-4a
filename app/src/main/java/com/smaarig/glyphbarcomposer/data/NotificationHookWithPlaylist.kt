package com.smaarig.glyphbarcomposer.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation that joins a [NotificationHook] with its associated [Playlist].
 *
 * Used by the DAO to return hooks together with their playlist in a single query,
 * avoiding manual joins in the repository or ViewModel.
 *
 * [playlist] is nullable because the playlist could have been deleted while the
 * hook still exists (Room CASCADE on the FK will normally prevent this, but
 * defensive nullability avoids crashes during the brief window between deletion
 * and the next DB sync, and is required by Room for @Relation fields).
 *
 * Usage in DAO:
 * ```kotlin
 * @Transaction
 * @Query("SELECT * FROM notification_hooks WHERE isEnabled = 1")
 * fun getAllHooksWithPlaylist(): Flow<List<NotificationHookWithPlaylist>>
 * ```
 */
data class NotificationHookWithPlaylist(

    @Embedded
    val hook: NotificationHook,

    @Relation(
        parentColumn = "playlistId",   // column in notification_hooks
        entityColumn = "id"            // column in playlists
    )
    val playlist: Playlist?
)