package com.smaarig.glyphbarcomposer

/**
 * Data class to store a set of Glyph channels with their intensities and a duration for playback.
 * Intensities are typically 0 (OFF) to 3 (Full ON).
 */
data class GlyphSequence(
    val channelIntensities: Map<Int, Int>,
    val durationMs: Int
) {
    // Helper to get active channels for legacy or simple controllers
    val activeChannels: List<Int> get() = channelIntensities.filter { it.value > 0 }.keys.toList()
}
