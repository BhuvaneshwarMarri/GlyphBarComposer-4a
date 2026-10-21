package com.smaarig.glyphbarcomposer.controller

import com.smaarig.glyphbarcomposer.controller.GlyphController.GlyphOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class GlyphControllerSyncTest {

    @Test
    fun `glyphOwner values are defined correctly`() {
        val values = GlyphOwner.values()
        assertEquals(7, values.size)
        assertEquals(GlyphOwner.NONE, values[0])
        assertEquals(GlyphOwner.COMPOSER, values[1])
        assertEquals(GlyphOwner.STUDIO, values[2])
        assertEquals(GlyphOwner.PATTERN_LAB, values[3])
        assertEquals(GlyphOwner.BATTERY, values[4])
        assertEquals(GlyphOwner.SERVICE, values[5])
        assertEquals(GlyphOwner.WIDGET, values[6])
    }
}
