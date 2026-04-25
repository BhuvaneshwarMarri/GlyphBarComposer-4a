package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.studio.rememberStudioTimelineState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

import androidx.compose.ui.tooling.preview.Preview
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 800, heightDp = 300)
@Composable
fun StudioTimelineEditorPreview() {
    val uiState = MusicStudioUiState(
        audioDurationMs = 10000,
        musicEvents = listOf(
            MusicStudioEvent(id = 1, projectId = 1, timestampMs = 1000, channelIntensities = mapOf(1 to 3), durationMs = 1000),
            MusicStudioEvent(id = 2, projectId = 1, timestampMs = 3000, channelIntensities = mapOf(2 to 2), durationMs = 500)
        )
    )
    StudioTimelineEditor(
        uiState = uiState,
        audioPositionMs = 2000
    )
}

@Composable
fun StudioTimelineEditor(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    onSelectEvent: (MusicStudioEvent?) -> Unit = {},
    onMoveEvent: (MusicStudioEvent, Long) -> Unit = { _, _ -> },
    onResizeEvent: (MusicStudioEvent, Long, Int) -> Unit = { _, _, _ -> },
    onDeleteEvent: (MusicStudioEvent) -> Unit = {},
    onSeekMusic: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineState = rememberStudioTimelineState()

    LaunchedEffect(audioPositionMs, uiState.isAudioPlaying) {
        if (!uiState.isAudioPlaying) return@LaunchedEffect
        val px = timelineState.timeToPx(audioPositionMs.toLong())
        val start = timelineState.horizontalScrollState.value
        val width = timelineState.horizontalScrollState.viewportSize
        if (px > start + width * 0.70f || px < start + width * 0.10f) {
            timelineState.horizontalScrollState.animateScrollTo(
                (px - width * 0.25f).toInt().coerceAtLeast(0),
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("StudioTimelineEditor"),
        color = Color(0xFF080808),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF1A1A1A))
    ) {
        Row(Modifier.fillMaxSize()) {
            // Vertical track labels
            Row(
                modifier = Modifier
                    .width(TRACK_LABEL_WIDTH)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0C0C))
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.height(WAVEFORM_HEIGHT).fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Text("WAVE", color = Color(0xFF222222), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PATTERNS", color = Color(0xFF333333), fontSize = 9.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer(rotationZ = -90f))
                    }
                }
                VerticalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .horizontalScroll(timelineState.horizontalScrollState)
                    .verticalScroll(timelineState.verticalScrollState)
            ) {
                val totalWidth = timelineState.timeToDp(uiState.audioDurationMs.toLong())
                Box(modifier = Modifier.width(totalWidth + 160.dp).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(WAVEFORM_HEIGHT).background(Color(0xFF060606))) {
                            WaveformRuler(uiState.waveform, Modifier.fillMaxSize())
                        }
                        HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
                        
                        SequenceTrack(
                            events = uiState.musicEvents,
                            state = timelineState,
                            selectedEventId = uiState.selectedEventId,
                            onSelectEvent = onSelectEvent,
                            onMoveEvent = onMoveEvent,
                            onResizeEvent = onResizeEvent,
                            onDeleteEvent = onDeleteEvent,
                            modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT)
                        )
                    }

                    // Playhead
                    Box(
                        modifier = Modifier
                            .testTag("Playhead")
                            .graphicsLayer {
                                translationX = timelineState.timeToPx(audioPositionMs.toLong()) - 10.dp.toPx()
                            }
                            .width(20.dp) // Large hit target
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    onSeekMusic((audioPositionMs + timelineState.dragDeltaToMs(drag.x)).toFloat())
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // The visual line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF00C853).copy(0.8f))
                        )

                        Box(
                            modifier = Modifier.align(Alignment.TopCenter).size(16.dp)
                                .offset(y = (-2).dp).clip(CircleShape)
                                .background(Color(0xFF00C853))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
