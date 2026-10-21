package com.smaarig.glyphbarcomposer.controller

import com.nothing.ketchum.Glyph

/**
 * Single source of truth for all Phone (4a) (DEVICE_25111) channel constants.
 * Note: Glyph.Code_25111 only contains A_1 to A_6 in the current SDK.
 * The 7th (red/accent) bar is currently mapped to Glyph.Code_22111.E1 as a fallback.
 */
object GlyphConstants {
    val PHONE_4A_CHANNELS = listOf(
        Glyph.Code_25111.A_1,
        Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_6,
        Glyph.Code_22111.E1 // TODO: verify the correct red/accent constant for 4a (Code_25111 equivalent)
    )

    fun getChannelForIndex(index: Int): Int = when (index) {
        0 -> Glyph.Code_25111.A_1
        1 -> Glyph.Code_25111.A_2
        2 -> Glyph.Code_25111.A_3
        3 -> Glyph.Code_25111.A_4
        4 -> Glyph.Code_25111.A_5
        5 -> Glyph.Code_25111.A_6
        6 -> PHONE_4A_CHANNELS[6]
        else -> 0
    }
}

/**
 * Extension on Map<Int, Int> representing channel intensities.
 * Safely looks up intensity (0..3) for channel at 0-based index (0..6).
 * Handles maps keyed by either GlyphConstants.PHONE_4A_CHANNELS[index] or 0-based index (0..6).
 */
fun Map<Int, Int>.getIntensityForIndex(index: Int): Int {
    val ch = GlyphConstants.getChannelForIndex(index)
    return this[ch] ?: this[index] ?: 0
}

/**
 * Extension on Map<Int, Int> representing channel intensities.
 * Safely looks up intensity (0..3) for the given channel code constant.
 * Handles maps keyed by either channel code or 0-based index.
 */
fun Map<Int, Int>.getIntensityForChannel(channelCode: Int): Int {
    val idx = GlyphConstants.PHONE_4A_CHANNELS.indexOf(channelCode)
    return this[channelCode] ?: (if (idx >= 0) this[idx] else null) ?: 0
}

/**
 * Converts any channel intensity map (whether keyed by 0..6 or channel codes)
 * into a standardized map where keys are always GlyphConstants.PHONE_4A_CHANNELS[i].
 */
fun Map<Int, Int>.toNormalizedChannelMap(): Map<Int, Int> {
    val result = mutableMapOf<Int, Int>()
    for (i in 0..6) {
        val intensity = getIntensityForIndex(i)
        if (intensity > 0) {
            result[GlyphConstants.getChannelForIndex(i)] = intensity
        }
    }
    return result
}
