package com.smaarig.glyphbarcomposer.viewmodel

import android.app.Application
import android.util.Log
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.BeatAlgorithm
import io.mockk.*
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
import android.media.MediaPlayer
import android.net.Uri

@OptIn(ExperimentalCoroutinesApi::class)
class MusicStudioViewModelTest {

    private lateinit var viewModel: MusicStudioViewModel
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
        
        mockkConstructor(MediaPlayer::class)
        every { anyConstructed<MediaPlayer>().setDataSource(any<Application>(), any()) } just Runs
        
        viewModel = MusicStudioViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial algorithm is MANUAL_EDIT`() {
        assertEquals(BeatAlgorithm.MANUAL_EDIT, viewModel.uiState.value.selectedAlgorithm)
    }

    @Test
    fun `setAlgorithm updates selected algorithm`() {
        viewModel.setAlgorithm(BeatAlgorithm.PRO_SYNC_FFT)
        assertEquals(BeatAlgorithm.PRO_SYNC_FFT, viewModel.uiState.value.selectedAlgorithm)
    }

    @Test
    fun `setBpmOverride updates bpm`() {
        viewModel.setBpmOverride(140)
        assertEquals(140, viewModel.uiState.value.bpmOverride)
    }

    @Test
    fun `setDefaultDuration updates state`() {
        viewModel.setDefaultDuration(500)
        assertEquals(500, viewModel.uiState.value.defaultDurationMs)
    }

    @Test
    fun `toggleRedGlyph updates state`() {
        viewModel.toggleRedGlyph(false)
        assertEquals(false, viewModel.uiState.value.includeRedGlyph)
    }

    @Test
    fun `onComposerIntensityChange updates intensities`() {
        viewModel.onComposerIntensityChange(0, 3)
        assertEquals(3, viewModel.composerIntensities.value[0])
    }

    @Test
    fun `clearComposer resets intensities`() {
        viewModel.onComposerIntensityChange(0, 3)
        viewModel.clearComposer()
        assertEquals(0, viewModel.composerIntensities.value[0])
    }

    @Test
    fun `resetProject clears state`() {
        viewModel.onComposerIntensityChange(0, 3)
        viewModel.resetProject()
        assertEquals(null, viewModel.uiState.value.audioUri)
        assertEquals(0, viewModel.composerIntensities.value[0])
    }

    @Test
    fun `seekMusic handles clamping with zero duration`() {
        viewModel.seekMusic(1000f)
        assertEquals(0, viewModel.audioPositionMs.value)
    }

    @Test
    fun `setBpmOverride clamps value`() {
        viewModel.setBpmOverride(10) // below 40
        assertEquals(40, viewModel.uiState.value.bpmOverride)
        
        viewModel.setBpmOverride(500) // above 300
        assertEquals(300, viewModel.uiState.value.bpmOverride)
    }
}
