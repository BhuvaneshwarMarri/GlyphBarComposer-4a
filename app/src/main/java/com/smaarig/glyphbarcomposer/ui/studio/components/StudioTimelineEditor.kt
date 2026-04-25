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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.studio.rememberStudioTimelineState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

@Composable
fun StudioTimelineEditor(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    viewModel: MusicStudioViewModel,
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
        modifier = modifier.fillMaxWidth(),
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
                            onSelectEvent = viewModel::selectEvent,
                            onMoveEvent = viewModel::updateEventPosition,
                            onResizeEvent = viewModel::updateEventStartAndDuration,
                            onDeleteEvent = viewModel::deleteMusicEvent,
                            modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT)
                        )
                    }

                    // Playhead
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = timelineState.timeToPx(audioPositionMs.toLong())
                            }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF00C853).copy(0.8f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    viewModel.seekMusic((audioPositionMs + timelineState.dragDeltaToMs(drag.x)).toFloat())
                                }
                            }
                    ) {
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
