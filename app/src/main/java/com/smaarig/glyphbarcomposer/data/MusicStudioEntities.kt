package com.smaarig.glyphbarcomposer.data

import androidx.room.*

@Entity(tableName = "music_studio_projects")
data class MusicStudioProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val localAudioPath: String,
    val localGlyphPath: String? = null
)

@Entity(
    tableName = "music_studio_events",
    foreignKeys = [
        ForeignKey(
            entity = MusicStudioProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class MusicStudioEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val timestampMs: Long,
    val channelIntensities: Map<Int, Int>,
    val durationMs: Int = 100 
)

data class MusicProjectWithEvents(
    @Embedded val project: MusicStudioProject,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val events: List<MusicStudioEvent>
)
