package com.smaarig.glyphbarcomposer.utils

import com.smaarig.glyphbarcomposer.data.SequenceStep
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object GlyphSequenceImporter {

    @Serializable
    data class GlyphJsonFormat(
        val filename: String? = null,
        val version: String? = null,
        val sequences: List<List<Int>>
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun importFromJson(content: String): List<SequenceStep> {
        val data = try {
            json.decodeFromString<GlyphJsonFormat>(content)
        } catch (e: Exception) {
            // Try fallback to just List<List<Int>> if the root isn't the object
            try {
                val rawSequences = json.parseToJsonElement(content).jsonArray
                GlyphJsonFormat(sequences = rawSequences.map { it.jsonArray.map { p -> p.jsonPrimitive.content.toInt() } })
            } catch (e2: Exception) {
                throw IllegalArgumentException("Invalid JSON format: ${e.message}")
            }
        }

        return validateAndConvert(data.sequences)
    }

    fun importFromCsv(content: String): List<SequenceStep> {
        val allLines = content.lines().filter { it.isNotBlank() }
        if (allLines.isEmpty()) throw IllegalArgumentException("Sequence is empty")

        // Try to detect separator
        val firstLine = allLines[0]
        val separator = if (firstLine.contains(";")) ";" else ","

        // Detect and skip header
        val startIdx = if (firstLine.contains(Regex("[a-zA-Z]"))) 1 else 0
        val lines = allLines.drop(startIdx)

        val sequences = mutableListOf<List<Int>>()

        lines.forEachIndexed { index, line ->
            val parts = line.split(separator).map { it.trim() }
            
            // Format: idx,x1,x2,x3,x4,x5,x6,x7,duration (9 parts)
            // or x1,x2,x3,x4,x5,x6,x7,duration (8 parts)
            
            val values = when (parts.size) {
                9 -> parts.drop(1).map { it.toIntOrNull() ?: throw IllegalArgumentException("Line ${index + 1 + startIdx}: Non-integer value '$it'") }
                8 -> parts.map { it.toIntOrNull() ?: throw IllegalArgumentException("Line ${index + 1 + startIdx}: Non-integer value '$it'") }
                else -> throw IllegalArgumentException("Line ${index + 1 + startIdx}: Invalid number of columns (expected 8 or 9, got ${parts.size})")
            }
            sequences.add(values)
        }

        return validateAndConvert(sequences)
    }

    private fun validateAndConvert(sequences: List<List<Int>>): List<SequenceStep> {
        if (sequences.isEmpty()) throw IllegalArgumentException("Sequence is empty")

        return sequences.mapIndexed { stepIdx, row ->
            if (row.size != 8) {
                throw IllegalArgumentException("Step ${stepIdx + 1}: Expected 8 columns (x1-x7, duration), got ${row.size}")
            }

            val x1to6 = row.subList(0, 6)
            val x7 = row[6]
            val duration = row[7]

            // Validate x1-x6 (0-3)
            x1to6.forEachIndexed { i, v ->
                if (v !in 0..3) throw IllegalArgumentException("Step ${stepIdx + 1}: Intensity x${i + 1} must be 0-3 (got $v)")
            }

            // Validate x7 (0-1)
            if (x7 !in 0..1) throw IllegalArgumentException("Step ${stepIdx + 1}: Red Glyph (x7) must be 0-1 (got $x7)")

            // Validate duration (50-2000, multiple of 50)
            if (duration !in 50..2000) throw IllegalArgumentException("Step ${stepIdx + 1}: Duration must be 50-2000ms (got $duration)")
            if (duration % 50 != 0) throw IllegalArgumentException("Step ${stepIdx + 1}: Duration must be a multiple of 50ms (got $duration)")

            val intensities = mutableMapOf<Int, Int>()
            x1to6.forEachIndexed { i, v -> if (v > 0) intensities[i] = v }
            // Spec says x7 is 0-1, but our internal intensity is 0 or 3 (for red)
            // Actually, if x7 is 1, we map it to intensity 3 for index 6.
            if (x7 > 0) intensities[6] = 3

            SequenceStep(
                playlistId = 0,
                stepIndex = stepIdx,
                channelIntensities = intensities,
                durationMs = duration
            )
        }
    }
}
