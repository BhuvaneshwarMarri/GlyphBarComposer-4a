package com.smaarig.glyphbarcomposer.data

import androidx.room.*
import com.smaarig.glyphbarcomposer.model.GlyphSequence

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "sequence_steps",
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
data class SequenceStep(
    @PrimaryKey(autoGenerate = true) val stepId: Long = 0,
    val playlistId: Long,
    val stepIndex: Int,
    val channelIntensities: Map<Int, Int>,
    val durationMs: Int
) {
    // Legacy support property
    val activeChannels: List<Int> get() = channelIntensities.filter { it.value > 0 }.keys.toList()
}

data class PlaylistWithSteps(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val steps: List<SequenceStep>
)

class Converters {
    @TypeConverter
    fun fromMap(map: Map<Int, Int>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toMap(data: String): Map<Int, Int> {
        if (data.isEmpty()) return emptyMap()
        return data.split(",").associate {
            val parts = it.split(":")
            parts[0].toInt() to parts[1].toInt()
        }
    }

    // Keep legacy for safety if still needed
    @TypeConverter
    fun fromList(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String): List<Int> {
        if (data.isEmpty()) return emptyList()
        return data.split(",").map { it.toInt() }
    }
}
