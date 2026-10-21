package com.smaarig.glyphbarcomposer.utils

import com.smaarig.glyphbarcomposer.controller.GlyphConstants
import com.smaarig.glyphbarcomposer.data.SequenceStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSequenceExporterTest {

    @Test
    fun `exportToCsv generates correct format with index keys`() {
        val steps = listOf(
            SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = mapOf(0 to 1, 6 to 3), durationMs = 500)
        )
        val csv = GlyphSequenceExporter.exportToCsv(steps)
        // Format: idx,x1,x2,x3,x4,x5,x6,x7,duration
        assertEquals("0,1,0,0,0,0,0,1,500\n", csv)
    }

    @Test
    fun `exportToCsv generates correct format with channel code keys`() {
        val steps = listOf(
            SequenceStep(
                playlistId = 0,
                stepIndex = 0,
                channelIntensities = mapOf(
                    GlyphConstants.PHONE_4A_CHANNELS[0] to 2,
                    GlyphConstants.PHONE_4A_CHANNELS[3] to 3,
                    GlyphConstants.PHONE_4A_CHANNELS[6] to 3
                ),
                durationMs = 250
            )
        )
        val csv = GlyphSequenceExporter.exportToCsv(steps)
        assertEquals("0,2,0,0,3,0,0,1,250\n", csv)
    }

    @Test
    fun `exportToJson generates correct format`() {
        val steps = listOf(
            SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = mapOf(2 to 3, 6 to 3), durationMs = 1000)
        )
        val json = GlyphSequenceExporter.exportToJson("test", steps)
        assertTrue(json.contains("\"filename\": \"test\""))
        assertTrue(json.contains("3"))
        assertTrue(json.contains("1000"))
        assertTrue(json.contains("1"))
    }

    @Test
    fun `exportToJson generates correct format with channel code keys`() {
        val steps = listOf(
            SequenceStep(
                playlistId = 0,
                stepIndex = 0,
                channelIntensities = mapOf(
                    GlyphConstants.PHONE_4A_CHANNELS[1] to 3,
                    GlyphConstants.PHONE_4A_CHANNELS[6] to 3
                ),
                durationMs = 500
            )
        )
        val json = GlyphSequenceExporter.exportToJson("test_channel_codes", steps)
        assertTrue(json.contains("\"filename\": \"test_channel_codes\""))
        assertTrue(json.contains("3"))
        assertTrue(json.contains("500"))
    }
}
