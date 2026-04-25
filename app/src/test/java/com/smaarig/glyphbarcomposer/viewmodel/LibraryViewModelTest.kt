package com.smaarig.glyphbarcomposer.viewmodel

import android.app.Application
import android.util.Log
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private lateinit var viewModel: LibraryViewModel
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<GlyphRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { repository.allPlaylists } returns flowOf(emptyList())
        every { repository.allMusicProjects } returns flowOf(emptyList())
        
        viewModel = LibraryViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deletePlaylist calls repository`() = runTest {
        val playlist = Playlist(id = 1, name = "Test")
        viewModel.deletePlaylist(playlist)
        coVerify { repository.deletePlaylist(playlist) }
    }
}
