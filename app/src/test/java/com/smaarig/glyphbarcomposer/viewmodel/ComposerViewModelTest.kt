package com.smaarig.glyphbarcomposer.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerViewModelTest {

    private lateinit var viewModel: ComposerViewModel
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<GlyphRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        
        // Mock SharedPreferences for PreferenceManager inside ViewModel
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { application.getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.getBoolean("use_old_version", true) } returns true
        
        viewModel = ComposerViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals(1000f, state.durationMs)
        assertTrue(state.currentSequenceSteps.isEmpty())
        assertTrue(state.useOldVersion) // V1 is default
    }

    @Test
    fun `toggleVersion updates state`() {
        viewModel.toggleVersion(false)
        assertEquals(false, viewModel.uiState.value.useOldVersion)
        
        viewModel.toggleVersion(true)
        assertEquals(true, viewModel.uiState.value.useOldVersion)
    }

    @Test
    fun `onIntensityChange updates intensities`() {
        viewModel.onIntensityChange(0, 3)
        assertEquals(3, viewModel.uiState.value.glyphIntensities[0])
    }

    @Test
    fun `addStep adds a new step to sequence`() {
        viewModel.onIntensityChange(0, 3)
        viewModel.onDurationChange(500f)
        viewModel.addStep()
        
        val steps = viewModel.uiState.value.currentSequenceSteps
        assertEquals(1, steps.size)
        assertEquals(500, steps[0].durationMs)
    }

    @Test
    fun `removeStep removes step at index`() {
        viewModel.addStep() // step 0
        viewModel.addStep() // step 1
        assertEquals(2, viewModel.uiState.value.currentSequenceSteps.size)
        
        viewModel.removeStep(0)
        assertEquals(1, viewModel.uiState.value.currentSequenceSteps.size)
    }

    @Test
    fun `clearSequence empties steps`() {
        viewModel.addStep()
        viewModel.addStep()
        viewModel.clearSequence()
        assertTrue(viewModel.uiState.value.currentSequenceSteps.isEmpty())
    }

    @Test
    fun `reorderSteps moves step correctly`() {
        viewModel.onDurationChange(100f)
        viewModel.addStep() // step 0 (100ms)
        viewModel.onDurationChange(200f)
        viewModel.addStep() // step 1 (200ms)
        
        viewModel.reorderSteps(0, 1)
        val steps = viewModel.uiState.value.currentSequenceSteps
        assertEquals(200, steps[0].durationMs)
        assertEquals(100, steps[1].durationMs)
    }

    @Test
    fun `onDurationChange updates state`() {
        viewModel.onDurationChange(1500f)
        assertEquals(1500f, viewModel.uiState.value.durationMs)
    }

    @Test
    fun `loadStep updates intensities and duration`() {
        viewModel.onIntensityChange(1, 2)
        viewModel.onDurationChange(800f)
        viewModel.addStep()
        
        viewModel.loadStep(0)
        assertEquals(800f, viewModel.uiState.value.durationMs)
        assertEquals(2, viewModel.uiState.value.glyphIntensities[1])
    }

    @Test
    fun `stopPlayback resets playing state and intensities`() {
        viewModel.onIntensityChange(0, 3)
        viewModel.stopPlayback()
        
        val state = viewModel.uiState.value
        assertEquals(false, state.isPlaying)
        assertEquals(0, state.glyphIntensities[0])
    }

    @Test
    fun `savePlaylist with empty name does nothing`() {
        viewModel.addStep()
        viewModel.savePlaylist("")
        // Verify repository was not called
        coVerify(exactly = 0) { repository.savePlaylist(any(), any()) }
    }

    @Test
    fun `savePlaylist with valid name calls repository and clears sequence`() {
        viewModel.addStep()
        viewModel.savePlaylist("Test Playlist")
        
        coVerify { repository.savePlaylist(any(), any()) }
        assertTrue(viewModel.uiState.value.currentSequenceSteps.isEmpty())
    }

    @Test
    fun `togglePause toggles isPaused if isPlaying`() {
        viewModel.addStep()
        viewModel.startPlayback(viewModel.uiState.value.currentSequenceSteps)
        
        assertEquals(true, viewModel.uiState.value.isPlaying)
        assertEquals(false, viewModel.uiState.value.isPaused)
        
        viewModel.togglePause()
        assertEquals(true, viewModel.uiState.value.isPaused)
        
        viewModel.togglePause()
        assertEquals(false, viewModel.uiState.value.isPaused)
    }

    @Test
    fun `playSequence starts playback if not currently playing that playlist`() {
        val playlist = Playlist(id = 1, name = "Test")
        val steps = listOf(SequenceStep(stepId = 0, playlistId = 1, stepIndex = 0, channelIntensities = mapOf(1 to 3), durationMs = 500))
        val playlistWithSteps = PlaylistWithSteps(playlist, steps)
        
        viewModel.playSequence(playlistWithSteps)
        
        val state = viewModel.uiState.value
        assertEquals(true, state.isPlaying)
        assertEquals(1L, state.activePlaylistId)
    }
}
