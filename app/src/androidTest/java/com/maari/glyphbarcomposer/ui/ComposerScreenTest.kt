package com.maari.glyphbarcomposer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.maari.glyphbarcomposer.ui.composer.ComposerScreen
import com.maari.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.maari.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.maari.glyphbarcomposer.ui.viewmodel.PlaybackState
import com.maari.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ComposerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uiStateFlow = MutableStateFlow(ComposerUiState())
    private val playbackStateFlow = MutableStateFlow(PlaybackState())
    private val viewModel = mockk<ComposerViewModel>(relaxed = true).apply {
        every { uiState } returns uiStateFlow
        every { playbackState } returns playbackStateFlow
    }
    private val redViewModel = mockk<RedGlyphViewModel>(relaxed = true)

    @Test
    fun composerScreen_displaysHeader() {
        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        composeTestRule.onNodeWithText("COMPOSER").assertIsDisplayed()
    }
}
