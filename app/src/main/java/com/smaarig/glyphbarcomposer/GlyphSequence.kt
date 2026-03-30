package com.smaarig.glyphbarcomposer

/**
 * Data class to store a set of active Glyph channels and a duration for playback.
 */
data class GlyphSequence(
    val activeChannels: List<Int>,
    val durationMs: Int
)
