package com.smaarig.glyphbarcomposer.repository

import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistDao
import com.smaarig.glyphbarcomposer.data.SequenceStep
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GlyphRepositoryTest {

    private lateinit var repository: GlyphRepository
    private val playlistDao = mockk<PlaylistDao>(relaxed = true)

    @Before
    fun setup() {
        repository = GlyphRepository(playlistDao)
    }

    @Test
    fun `savePlaylist inserts playlist and steps`() = runTest {
        val playlist = Playlist(name = "Test")
        val steps = listOf(SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = emptyMap(), durationMs = 100))
        
        repository.savePlaylist(playlist, steps)
        
        coVerify { playlistDao.insertPlaylist(playlist) }
        coVerify { playlistDao.insertSteps(any()) }
    }

    @Test
    fun `deletePlaylist calls dao`() = runTest {
        val playlist = Playlist(name = "Test")
        repository.deletePlaylist(playlist)
        coVerify { playlistDao.deletePlaylist(playlist) }
    }
}
