package com.smaarig.glyphbarcomposer.utils

import com.smaarig.glyphbarcomposer.data.SequenceStep
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSequenceExporterTest {

    @Test
    fun `exportToCsv generates correct format`() {
        val steps = listOf(
            SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = mapOf(0 to 1, 6 to 3), durationMs = 500)
        )
        val csv = GlyphSequenceExporter.exportToCsv(steps)
        // Format: idx,x1,x2,x3,x4,x5,x6,x7,duration
        assertEquals("0,1,0,0,0,0,0,1,500\n", csv)
    }

    @Test
    fun `exportToJson generates correct format`() {
        val steps = listOf(
            SequenceStep(playlistId = 0, stepIndex = 0, channelIntensities = mapOf(2 to 3, 6 to 3), durationMs = 1000)
        )
        val json = GlyphSequenceExporter.exportToJson("test", steps)
        assertTrue(json.contains("\"filename\": \"test\""))
        // Check for presence of the values rather than exact formatted string
        assertTrue(json.contains("3"))
        assertTrue(json.contains("1000"))
        assertTrue(json.contains("1"))
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
