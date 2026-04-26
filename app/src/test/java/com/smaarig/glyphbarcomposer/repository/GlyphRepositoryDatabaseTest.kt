package com.smaarig.glyphbarcomposer.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistDao
import com.smaarig.glyphbarcomposer.data.SequenceStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlyphRepositoryDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var repository: GlyphRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = database.playlistDao()
        repository = GlyphRepository(playlistDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `save and load playlist works`() = runTest {
        val playlist = Playlist(name = "Test DB")
        val steps = listOf(SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = mapOf(1 to 3), durationMs = 100))
        
        repository.savePlaylist(playlist, steps)
        
        val allPlaylists = repository.allPlaylists.first()
        assertEquals(1, allPlaylists.size)
        assertEquals("Test DB", allPlaylists[0].playlist.name)
        assertEquals(1, allPlaylists[0].steps.size)
        assertEquals(3, allPlaylists[0].steps[0].channelIntensities[1])
    }
}
