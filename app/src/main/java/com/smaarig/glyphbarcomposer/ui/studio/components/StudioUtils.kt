package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.ui.unit.dp

val TRACK_LABEL_WIDTH = 48.dp
val TRACK_HEIGHT = 180.dp
val WAVEFORM_HEIGHT = 48.dp
val RESIZE_HANDLE_WIDTH = 14.dp
val MIN_BLOCK_WIDTH = 24.dp

fun formatTime(ms: Int): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}
