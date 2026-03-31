package com.smaarig.glyphbarcomposer.data

import androidx.room.*

@Entity(
    tableName = "event_bindings",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class EventBinding(
    @PrimaryKey val eventId: String, // e.g., "ALARM", "CALL", "MSG_MOM"
    val playlistId: Long,
    val isEnabled: Boolean = true
)

data class EventBindingWithPlaylist(
    @Embedded val binding: EventBinding,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id"
    )
    val playlist: Playlist?
)
