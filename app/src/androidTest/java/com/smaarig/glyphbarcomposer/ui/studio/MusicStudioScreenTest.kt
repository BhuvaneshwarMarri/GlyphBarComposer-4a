package com.smaarig.glyphbarcomposer.ui.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MusicStudioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<MusicStudioViewModel>(relaxed = true)
    private val uiState = MutableStateFlow(MusicStudioUiState())

    @Test
    fun musicStudioScreen_displaysSetupInitially() {
        every { viewModel.uiState } returns uiState
        every { viewModel.visualizerData } returns MutableStateFlow(emptyList())
        every { viewModel.audioPositionMs } returns MutableStateFlow(0)
        every { viewModel.composerIntensities } returns MutableStateFlow(listOf(0,0,0,0,0,0,0))
        every { viewModel.liveGlyphIntensities } returns MutableStateFlow(listOf(0,0,0,0,0,0,0))

        composeTestRule.setContent {
            MaterialTheme {
                MusicStudioScreen(viewModel = viewModel)
            }
        }

        // Check for common setup text
        composeTestRule.onNodeWithText("TRACK", substring = true).assertIsDisplayed()
    }
}
