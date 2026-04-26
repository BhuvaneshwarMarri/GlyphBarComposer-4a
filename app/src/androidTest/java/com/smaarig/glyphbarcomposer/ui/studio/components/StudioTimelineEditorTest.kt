package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class StudioTimelineEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<MusicStudioViewModel>(relaxed = true)

    @Test
    fun studioTimelineEditor_rendersLabels() {
        val uiState = MusicStudioUiState(audioDurationMs = 5000)
        
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StudioTimelineEditor(
                        uiState = uiState,
                        audioPositionMs = 0,
                        onSelectEvent = viewModel::selectEvent,
                        onMoveEvent = viewModel::updateEventPosition,
                        onResizeEvent = viewModel::updateEventStartAndDuration,
                        onDeleteEvent = viewModel::deleteMusicEvent,
                        onSeekMusic = viewModel::seekMusic
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("WAVE").assertIsDisplayed()
        composeTestRule.onNodeWithText("PATTERNS").assertIsDisplayed()
    }

    @Test
    fun studioTimelineEditor_displaysEvents() {
        val event = MusicStudioEvent(id = 1, projectId = 1, timestampMs = 1000, channelIntensities = mapOf(1 to 3), durationMs = 500)
        val uiState = MusicStudioUiState(
            audioDurationMs = 5000,
            musicEvents = listOf(event)
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StudioTimelineEditor(
                        uiState = uiState,
                        audioPositionMs = 0,
                        onSelectEvent = viewModel::selectEvent,
                        onMoveEvent = viewModel::updateEventPosition,
                        onResizeEvent = viewModel::updateEventStartAndDuration,
                        onDeleteEvent = viewModel::deleteMusicEvent,
                        onSeekMusic = viewModel::seekMusic
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("SequenceBlock_1").assertIsDisplayed()
    }

    @Test
    fun studioTimelineEditor_selectEvent_triggersViewModel() {
        val event = MusicStudioEvent(id = 1, projectId = 1, timestampMs = 1000, channelIntensities = mapOf(1 to 3), durationMs = 500)
        val uiState = MusicStudioUiState(
            audioDurationMs = 5000,
            musicEvents = listOf(event)
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StudioTimelineEditor(
                        uiState = uiState,
                        audioPositionMs = 0,
                        onSelectEvent = viewModel::selectEvent,
                        onMoveEvent = viewModel::updateEventPosition,
                        onResizeEvent = viewModel::updateEventStartAndDuration,
                        onDeleteEvent = viewModel::deleteMusicEvent,
                        onSeekMusic = viewModel::seekMusic
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("SequenceBlock_1").performClick()
        verify { viewModel.selectEvent(event) }
    }

    @Test
    fun studioTimelineEditor_playheadDragging_seeksMusic() {
        val uiState = MusicStudioUiState(audioDurationMs = 5000)

        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StudioTimelineEditor(
                        uiState = uiState,
                        audioPositionMs = 1000,
                        onSelectEvent = viewModel::selectEvent,
                        onMoveEvent = viewModel::updateEventPosition,
                        onResizeEvent = viewModel::updateEventStartAndDuration,
                        onDeleteEvent = viewModel::deleteMusicEvent,
                        onSeekMusic = viewModel::seekMusic
                    )
                }
            }
        }

        // Drag the playhead manually from its center
        composeTestRule.onNodeWithTag("Playhead").performTouchInput {
            swipe(center, center + Offset(100f, 0f), durationMillis = 500)
        }

        verify { viewModel.seekMusic(any()) }
    }

    @Test
    fun studioTimelineEditor_eventDragging_movesEvent() {
        val event = MusicStudioEvent(id = 1, projectId = 1, timestampMs = 1000, channelIntensities = mapOf(1 to 3), durationMs = 500)
        val uiState = MusicStudioUiState(
            audioDurationMs = 5000,
            musicEvents = listOf(event)
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StudioTimelineEditor(
                        uiState = uiState,
                        audioPositionMs = 0,
                        onSelectEvent = viewModel::selectEvent,
                        onMoveEvent = viewModel::updateEventPosition,
                        onResizeEvent = viewModel::updateEventStartAndDuration,
                        onDeleteEvent = viewModel::deleteMusicEvent,
                        onSeekMusic = viewModel::seekMusic
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("SequenceBlock_1").performTouchInput {
            swipeRight(startX = 50f, endX = 150f)
        }

        verify { viewModel.updateEventPosition(event, any()) }
    }
}
