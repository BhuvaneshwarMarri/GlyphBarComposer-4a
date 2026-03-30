package com.smaarig.glyphbarcomposer

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
}