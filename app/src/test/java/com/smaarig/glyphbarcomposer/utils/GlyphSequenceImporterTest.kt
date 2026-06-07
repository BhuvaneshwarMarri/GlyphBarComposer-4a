package com.smaarig.glyphbarcomposer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class GlyphSequenceImporterTest {

    @Test
    fun `importFromCsv parses valid 9-column CSV`() {
        val csv = "0,1,0,3,0,2,1,0,500\n1,3,3,3,3,3,3,1,250"
        val steps = GlyphSequenceImporter.importFromCsv(csv)
        
        assertEquals(2, steps.size)
        
        // First step
        assertEquals(500, steps[0].durationMs)
        assertEquals(1, steps[0].channelIntensities[0])
        assertEquals(3, steps[0].channelIntensities[2])
        assertEquals(2, steps[0].channelIntensities[4])
        assertEquals(1, steps[0].channelIntensities[5])
        assertEquals(null, steps[0].channelIntensities[6]) // x7 was 0
        
        // Second step
        assertEquals(250, steps[1].durationMs)
        assertEquals(3, steps[1].channelIntensities[0])
        assertEquals(3, steps[1].channelIntensities[6]) // x7 was 1, maps to 3
    }

    @Test
    fun `importFromCsv parses valid 8-column CSV`() {
        val csv = "1,0,3,0,2,1,0,500"
        val steps = GlyphSequenceImporter.importFromCsv(csv)
        assertEquals(1, steps.size)
        assertEquals(500, steps[0].durationMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importFromCsv rejects invalid intensity`() {
        val csv = "0,4,0,0,0,0,0,0,500"
        GlyphSequenceImporter.importFromCsv(csv)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importFromCsv rejects invalid red glyph intensity`() {
        val csv = "0,1,0,0,0,0,0,2,500"
        GlyphSequenceImporter.importFromCsv(csv)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importFromCsv rejects invalid duration range`() {
        val csv = "0,1,0,0,0,0,0,0,2500"
        GlyphSequenceImporter.importFromCsv(csv)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importFromCsv rejects duration not multiple of 50`() {
        val csv = "0,1,0,0,0,0,0,0,125"
        GlyphSequenceImporter.importFromCsv(csv)
    }

    @Test
    fun `importFromJson parses valid JSON`() {
        val json = """
            {
                "filename": "test",
                "sequences": [
                    [1,0,3,0,2,1,0,500],
                    [3,3,3,3,3,3,1,250]
                ]
            }
        """.trimIndent()
        val steps = GlyphSequenceImporter.importFromJson(json)
        assertEquals(2, steps.size)
        assertEquals(500, steps[0].durationMs)
        assertEquals(250, steps[1].durationMs)
        assertEquals(3, steps[1].channelIntensities[6])
    }

    @Test
    fun `importFromJson parses raw array JSON`() {
        val json = "[[1,0,3,0,2,1,0,500]]"
        val steps = GlyphSequenceImporter.importFromJson(json)
        assertEquals(1, steps.size)
    }
}
