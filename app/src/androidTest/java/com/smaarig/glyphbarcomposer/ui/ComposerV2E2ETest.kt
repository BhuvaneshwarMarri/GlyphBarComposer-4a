package com.smaarig.glyphbarcomposer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smaarig.glyphbarcomposer.ui.composer.ComposerScreen
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.RedGlyphViewModel
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ComposerV2E2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uiStateFlow = MutableStateFlow(ComposerUiState(useOldVersion = false))
    private val viewModel = mockk<ComposerViewModel>(relaxed = true).apply {
        every { uiState } returns uiStateFlow
    }
    private val redViewModel = mockk<RedGlyphViewModel>(relaxed = true)

    @Test
    fun testAddStepsAndAutoScroll() {
        // Setup initial state
        uiStateFlow.value = ComposerUiState(useOldVersion = false)

        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        // Click Add Step 5 times
        repeat(5) {
            composeTestRule.onNodeWithTag("add_step_button").performClick()
            
            // Update mock state manually since we are using a mock VM
            val currentSteps = uiStateFlow.value.currentSequenceSteps.toMutableList()
            currentSteps.add(GlyphSequence(emptyMap(), 500))
            uiStateFlow.value = uiStateFlow.value.copy(currentSequenceSteps = currentSteps)
        }

        // Verify 5th step is displayed (proving auto-scroll moved the list)
        composeTestRule.onNodeWithTag("step_box_4").assertIsDisplayed()
        
        // Verify ViewModel was called
        verify(exactly = 5) { viewModel.addStep() }
    }

    @Test
    fun testInteractionAndStepAddition() {
        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        // 1. Change duration
        composeTestRule.onNodeWithTag("duration_slider").performTouchInput {
            swipeRight()
        }
        verify { viewModel.onDurationChange(any()) }

        // 2. Add Step
        composeTestRule.onNodeWithTag("add_step_button").performClick()
        verify { viewModel.addStep() }

        // Verify that after addStep, the UI still shows the controls
        composeTestRule.onNodeWithTag("add_step_button").assertExists()
    }

    @Test
    fun testStepActions_RetryAndDelete() {
        // Setup state with 2 steps
        uiStateFlow.value = ComposerUiState(
            useOldVersion = false,
            currentSequenceSteps = listOf(
                GlyphSequence(emptyMap(), 500),
                GlyphSequence(emptyMap(), 1000)
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        // Test Retry (Load Step)
        composeTestRule.onNodeWithTag("retry_button_0").performClick()
        verify { viewModel.loadStep(0) }

        // Test Delete
        // First click to open menu
        composeTestRule.onNodeWithTag("step_box_1").performClick()
        // Then click delete button
        composeTestRule.onNodeWithTag("delete_button_1").performClick()
        verify { viewModel.removeStep(1) }
    }

    @Test
    fun testDragAndDropReordering() {
        // Setup state with 3 steps to make reordering more meaningful
        uiStateFlow.value = ComposerUiState(
            useOldVersion = false,
            currentSequenceSteps = listOf(
                GlyphSequence(emptyMap(), 500),
                GlyphSequence(emptyMap(), 1000),
                GlyphSequence(emptyMap(), 1500)
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        // Perform drag on handle of first step downwards to the second position
        // The item height is 88dp + 6dp spacing = 94dp.
        // In DraggableTimeline, it uses 183f for itemHeightPx calculation (probably a bit high or including more)
        // Let's swipe down far enough to trigger a swap.
        composeTestRule.onNodeWithTag("drag_handle_0", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 500f)) // Move down significantly
            up()
        }

        // Verify reorder was called
        verify { viewModel.reorderSteps(0, any()) }
    }

    @Test
    fun testCompleteSequenceFlow() {
        uiStateFlow.value = ComposerUiState(useOldVersion = false)

        composeTestRule.setContent {
            MaterialTheme {
                ComposerScreen(viewModel = viewModel, redViewModel = redViewModel)
            }
        }

        // 1. Set Duration
        composeTestRule.onNodeWithTag("duration_slider").performTouchInput {
            swipeRight()
        }
        
        // 2. Add Step
        composeTestRule.onNodeWithTag("add_step_button").performClick()
        
        // Update state to reflect added step
        uiStateFlow.value = uiStateFlow.value.copy(
            currentSequenceSteps = listOf(GlyphSequence(emptyMap(), 1000))
        )

        // 3. Play Sequence
        composeTestRule.onNodeWithTag("play_button").performClick()
        verify { viewModel.startPlayback(any(), any()) }

        // Update state to isPlaying = true to test Stop button
        uiStateFlow.value = uiStateFlow.value.copy(isPlaying = true)
        composeTestRule.onNodeWithTag("play_button").performClick()
        verify { viewModel.stopPlayback() }
        
        uiStateFlow.value = uiStateFlow.value.copy(isPlaying = false)

        // 4. Save Sequence
        composeTestRule.onNodeWithTag("save_button").performClick()
        
        // Verify Dialog is shown
        composeTestRule.onNodeWithTag("save_dialog_input").assertIsDisplayed()
        
        // Type name
        composeTestRule.onNodeWithTag("save_dialog_input").performTextInput("My Awesome Sequence")
        
        // Click Save
        composeTestRule.onNodeWithTag("save_dialog_confirm").performClick()
        
        verify { viewModel.savePlaylist("My Awesome Sequence") }
    }
}
