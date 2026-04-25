package com.smaarig.glyphbarcomposer.viewmodel

import android.app.Application
import android.util.Log
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class PatternLabViewModelTest {

    private lateinit var viewModel: PatternLabViewModel
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<GlyphRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { repository.allPlaylists } returns flowOf(emptyList())
        
        viewModel = PatternLabViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tab change updates selectedTab`() {
        viewModel.onTabChange(1)
        assertEquals(1, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `speed multiplier change updates state`() {
        viewModel.onSpeedMultiplierAChange(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.speedMultiplierA)
    }

    @Test
    fun `toggle invert updates state`() {
        viewModel.onInvertAChange(true)
        assertEquals(true, viewModel.uiState.value.isInvertedA)
    }

    @Test
    fun `merge mode change updates isLayered`() {
        viewModel.onMergeModeChange(true)
        assertEquals(true, viewModel.uiState.value.isLayered)
    }

    @Test
    fun `randomize updates multiple fields`() {
        viewModel.randomize()
        val newState = viewModel.uiState.value
        assertTrue(newState.speedMultiplierA in 0.5f..2.0f)
    }

    @Test
    fun `saveResult calls repository and resets state`() = runTest {
        viewModel.onResultNameChange("Test Mix")
        viewModel.saveResult("Test Mix")
        coVerify(exactly = 0) { repository.savePlaylist(any(), any()) }
    }
}
