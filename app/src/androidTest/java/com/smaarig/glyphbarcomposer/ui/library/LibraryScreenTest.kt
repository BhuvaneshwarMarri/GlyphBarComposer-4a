package com.smaarig.glyphbarcomposer.ui.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smaarig.glyphbarcomposer.ui.viewmodel.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val libraryViewModel = mockk<LibraryViewModel>(relaxed = true)
    private val composerViewModel = mockk<ComposerViewModel>(relaxed = true)
    private val musicStudioViewModel = mockk<MusicStudioViewModel>(relaxed = true)

    private val composerUiState = MutableStateFlow(ComposerUiState())
    private val musicStudioUiState = MutableStateFlow(MusicStudioUiState())

    @Test
    fun libraryScreen_displaysTabs() {
        every { libraryViewModel.allPlaylists } returns MutableStateFlow(emptyList())
        every { libraryViewModel.allMusicProjects } returns MutableStateFlow(emptyList())
        every { composerViewModel.uiState } returns composerUiState
        every { musicStudioViewModel.uiState } returns musicStudioUiState

        composeTestRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    composerViewModel = composerViewModel,
                    musicStudioViewModel = musicStudioViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("SEQUENCES").assertIsDisplayed()
        composeTestRule.onNodeWithText("MUSIC STUDIO").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_switchesTabs() {
        every { libraryViewModel.allPlaylists } returns MutableStateFlow(emptyList())
        every { libraryViewModel.allMusicProjects } returns MutableStateFlow(emptyList())
        every { composerViewModel.uiState } returns composerUiState
        every { musicStudioViewModel.uiState } returns musicStudioUiState

        composeTestRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    composerViewModel = composerViewModel,
                    musicStudioViewModel = musicStudioViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("MUSIC STUDIO").performClick()
        // Check if Studio tab content is displayed (e.g., "No Projects" empty state)
        composeTestRule.onNodeWithText("No Projects").assertIsDisplayed()
    }
}
