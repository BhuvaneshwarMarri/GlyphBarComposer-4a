package com.smaarig.glyphbarcomposer.data

import androidx.room.*

@Entity(tableName = "music_sync_projects")
data class MusicSyncProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val localAudioPath: String,
    val localGlyphPath: String? = null
)

@Entity(
    tableName = "music_sync_events",
    foreignKeys = [
        ForeignKey(
            entity = MusicSyncProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class MusicSyncEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val timestampMs: Long,
    val channelIntensities: Map<Int, Int>,
    val durationMs: Int = 200 // Default quick flash for music sync
)

data class MusicProjectWithEvents(
    @Embedded val project: MusicSyncProject,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val events: List<MusicSyncEvent>
)
