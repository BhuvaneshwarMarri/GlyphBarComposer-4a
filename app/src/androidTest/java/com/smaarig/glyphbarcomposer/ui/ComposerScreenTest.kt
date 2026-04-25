package com.smaarig.glyphbarcomposer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.smaarig.glyphbarcomposer.ui.composer.ComposerScreen
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ComposerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uiStateFlow = MutableStateFlow(ComposerUiState())
    private val viewModel = mockk<ComposerViewModel>(relaxed = true).apply {
        every { uiState } returns uiStateFlow
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
