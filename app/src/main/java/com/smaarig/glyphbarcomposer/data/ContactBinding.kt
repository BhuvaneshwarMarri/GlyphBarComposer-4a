package com.smaarig.glyphbarcomposer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "contact_bindings")
data class ContactBinding(
    @PrimaryKey val contactId: String,
    val contactName: String,
    val playlistId: Long
)

data class ContactBindingWithPlaylist(
    @Embedded val binding: ContactBinding,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id"
    )
    val playlist: Playlist
)
