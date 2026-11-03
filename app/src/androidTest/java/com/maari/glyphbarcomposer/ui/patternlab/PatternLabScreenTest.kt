package com.maari.glyphbarcomposer.ui.patternlab

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.maari.glyphbarcomposer.ui.viewmodel.PatternLabUiState
import com.maari.glyphbarcomposer.ui.viewmodel.PatternLabViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class PatternLabScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<PatternLabViewModel>(relaxed = true)
    private val uiState = MutableStateFlow(PatternLabUiState())

    @Test
    fun patternLabScreen_displaysTabs() {
        every { viewModel.uiState } returns uiState
        every { viewModel.allPlaylists } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                PatternLabScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("BASE A").assertIsDisplayed()
        composeTestRule.onNodeWithText("BASE B").assertIsDisplayed()
        composeTestRule.onNodeWithText("MIXER").assertIsDisplayed()
    }
}
