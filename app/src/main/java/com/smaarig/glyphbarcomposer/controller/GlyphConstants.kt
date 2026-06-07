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
