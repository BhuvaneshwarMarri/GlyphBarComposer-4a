package com.smaarig.glyphbarcomposer.viewmodel

import android.app.Application
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.Playlist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
    private val glyphController = mockk<GlyphController>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock GlyphController
        mockkObject(GlyphController.Companion)
        mockkStatic(GlyphController::class)
        every { GlyphController.getInstance(any()) } returns glyphController
        every { GlyphController.Companion.getInstance(any()) } returns glyphController

        // Mock StateFlows in GlyphController
        every { glyphController.isPlaying } returns MutableStateFlow(false)
        every { glyphController.isPaused } returns MutableStateFlow(false)
        every { glyphController.currentIntensities } returns MutableStateFlow(List(7) { 0 })
        every { glyphController.activeOwner } returns MutableStateFlow(GlyphController.GlyphOwner.NONE)

        viewModel = ComposerViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `addStep appends new step to sequence`() {
        assertEquals(0, viewModel.uiState.value.currentSequenceSteps.size)
        viewModel.addStep()
        assertEquals(1, viewModel.uiState.value.currentSequenceSteps.size)
    }

    @Test
    fun `removeStep removes step at specified index`() {
        viewModel.addStep()
        viewModel.addStep()
        assertEquals(2, viewModel.uiState.value.currentSequenceSteps.size)

        viewModel.removeStep(0)
        assertEquals(1, viewModel.uiState.value.currentSequenceSteps.size)
    }

    @Test
    fun `onIntensityChange updates intensity for selected channel`() {
        viewModel.onIntensityChange(0, 3)
        assertEquals(3, viewModel.uiState.value.glyphIntensities[0])
    }

    @Test
    fun `onDurationChange updates duration`() {
        viewModel.onDurationChange(500f)
        assertEquals(500f, viewModel.uiState.value.durationMs, 0.01f)
    }

    @Test
    fun `reorderSteps moves step correctly`() {
        viewModel.onIntensityChange(0, 1)
        viewModel.addStep() // step 0

        viewModel.onIntensityChange(0, 2)
        viewModel.addStep() // step 1

        viewModel.reorderSteps(0, 1)

        val steps = viewModel.uiState.value.currentSequenceSteps
        // The second step added should now be at index 0
        assertEquals(2, steps[0].channelIntensities.values.firstOrNull() ?: 0)
    }

    @Test
    fun `clearSequence resets currentSequenceSteps`() {
        viewModel.addStep()
        assertEquals(1, viewModel.uiState.value.currentSequenceSteps.size)

        viewModel.clearSequence()
        assertEquals(0, viewModel.uiState.value.currentSequenceSteps.size)
    }

    @Test
    fun `stopPlayback calls controller stop`() {
        viewModel.stopPlayback()
        verify { glyphController.stopPlayback() }
        verify { glyphController.releaseControl(GlyphController.GlyphOwner.COMPOSER) }
    }

    @Test
    fun `savePlaylist saves data via repository and clears state`() = runTest {
        viewModel.addStep()
        viewModel.savePlaylist("Test Playlist")

        coVerify { repository.savePlaylist(any(), any()) }
        assertTrue(viewModel.uiState.value.currentSequenceSteps.isEmpty())
    }

    @Test
    fun `togglePause calls controller toggle`() {
        viewModel.togglePause()
        verify { glyphController.togglePausePlayback() }
    }

    @Test
    fun `playSequence starts playback if not currently playing that playlist`() {
        val playlist = Playlist(id = 1, name = "Test")
        val steps = listOf(SequenceStep(stepId = 0, playlistId = 1, stepIndex = 0, channelIntensities = mapOf(1 to 3), durationMs = 500))
        val playlistWithSteps = PlaylistWithSteps(playlist, steps)

        viewModel.playSequence(playlistWithSteps)

        verify { glyphController.acquireControl(GlyphController.GlyphOwner.COMPOSER) }
        verify { glyphController.playSequence(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `playSequence starts playback for second playlist when first playlist was playing`() {
        val playlistA = Playlist(id = 1, name = "Test A")
        val stepsA = listOf(SequenceStep(stepId = 0, playlistId = 1, stepIndex = 0, channelIntensities = mapOf(1 to 3), durationMs = 500))
        val playlistWithStepsA = PlaylistWithSteps(playlistA, stepsA)

        val playlistB = Playlist(id = 2, name = "Test B")
        val stepsB = listOf(SequenceStep(stepId = 0, playlistId = 2, stepIndex = 0, channelIntensities = mapOf(2 to 3), durationMs = 500))
        val playlistWithStepsB = PlaylistWithSteps(playlistB, stepsB)

        viewModel.playSequence(playlistWithStepsA)
        assertEquals(1L, viewModel.uiState.value.activePlaylistId)

        viewModel.playSequence(playlistWithStepsB)
        assertEquals(2L, viewModel.uiState.value.activePlaylistId)
    }
}
