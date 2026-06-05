package com.smaarig.glyphbarcomposer.utils

import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.model.ImportError
import com.smaarig.glyphbarcomposer.model.ImportResult
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Importer and exporter for GlyphBar sequences in CSV format (.gbseq.csv).
 */
object CsvSequenceImporter {

    private val LABEL_TO_CHANNEL = mapOf(
        "A1"  to Glyph.Code_25111.A_1,
        "A2"  to Glyph.Code_25111.A_2,
        "A3"  to Glyph.Code_25111.A_3,
        "A4"  to Glyph.Code_25111.A_4,
        "A5"  to Glyph.Code_25111.A_5,
        "A6"  to Glyph.Code_25111.A_6,
        "RED" to Glyph.Code_22111.E1
    )

    private val CHANNEL_TO_LABEL = LABEL_TO_CHANNEL.entries.associate { (k, v) -> v to k }

    /**
     * Parses a CSV input stream into an [ImportResult].
     */
    fun parse(inputStream: InputStream): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val errors = mutableListOf<ImportError>()

        var name = ""
        var formatVersion = ""
        var device = ""
        val steps = mutableListOf<GlyphSequence>()

        var lineNum = 0
        var headerFound = false

        try {
            reader.forEachLine { line ->
                lineNum++
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEachLine

                if (trimmed.startsWith("#")) {
                    if (lineNum == 1) {
                        if (trimmed != "# GlyphBar Sequence") {
                            errors.add(ImportError("CSV_MISSING_MAGIC", "Missing magic header '# GlyphBar Sequence'.", 1))
                        }
                        return@forEachLine
                    }

                    val parts = trimmed.substring(1).split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        when (key) {
                            "name" -> name = value
                            "format_version" -> formatVersion = value
                            "device" -> device = value
                        }
                    }
                } else if (!headerFound) {
                    val expectedHeader = "step_index,duration_ms,A1,A2,A3,A4,A5,A6,RED"
                    if (trimmed != expectedHeader) {
                        errors.add(ImportError("CSV_WRONG_COLUMNS", "Invalid column header. Expected: $expectedHeader", lineNum))
                        return@forEachLine
                    }
                    headerFound = true
                } else {
                    val cols = trimmed.split(",")
                    if (cols.size != 9) {
                        errors.add(ImportError("CSV_ROW_WRONG_COUNT", "Invalid column count. Expected 9, got ${cols.size}", lineNum))
                    } else {
                        try {
                            val stepIndex = cols[0].trim().toInt()
                            val duration = cols[1].trim().toInt()

                            // BUG-7 FIX: Validate stepIndex is sequential, matching the JSON importer.
                            // The old code parsed stepIndex but silently discarded it, so duplicate
                            // or missing indices (e.g. 0,1,1,3) were accepted without error.
                            val expectedIndex = steps.size
                            if (stepIndex != expectedIndex) {
                                errors.add(ImportError("CSV_STEP_INDEX_GAP", "Step index mismatch at line $lineNum: expected $expectedIndex, got $stepIndex.", lineNum))
                            }

                            if (duration < 50 || duration > 30000) {
                                errors.add(ImportError("CSV_STEP_DURATION_RANGE", "Duration must be 50-30000 ms.", lineNum))
                            }

                            val intensities = mutableMapOf<Int, Int>()
                            val labels = listOf("A1", "A2", "A3", "A4", "A5", "A6", "RED")
                            labels.forEachIndexed { i, label ->
                                val intensity = cols[i + 2].trim().toInt()
                                if (intensity !in 0..3) {
                                    errors.add(ImportError("CSV_STEP_BAD_INTENSITY", "Invalid intensity for $label (0-3).", lineNum))
                                } else {
                                    LABEL_TO_CHANNEL[label]?.let { intensities[it] = intensity }
                                }
                            }
                            steps.add(GlyphSequence(intensities, duration))
                        } catch (e: NumberFormatException) {
                            errors.add(ImportError("CSV_ROW_NOT_INTEGER", "Non-integer value found in data row.", lineNum))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult.Failure(listOf(ImportError("IO_ERROR", "Read error: ${e.message}")))
        }

        // Final validation
        if (formatVersion != "1.0") errors.add(ImportError("CSV_MISSING_HEADER_META", "Invalid or missing format_version: 1.0"))
        if (device != "phone_4a") errors.add(ImportError("CSV_MISSING_HEADER_META", "Invalid or missing device: phone_4a"))
        if (name.isBlank()) errors.add(ImportError("CSV_MISSING_HEADER_META", "Sequence name is required."))

        if (steps.isEmpty()) {
            errors.add(ImportError("CSV_NO_STEPS", "No data rows found."))
        } else if (steps.size > 500) {
            errors.add(ImportError("CSV_TOO_MANY_STEPS", "Maximum 500 steps allowed."))
        }

        return if (errors.isEmpty()) {
            ImportResult.Success(name, steps)
        } else {
            ImportResult.Failure(errors)
        }
    }

    /**
     * Exports a [PlaylistWithSteps] to a CSV string.
     */
    fun export(playlistWithSteps: PlaylistWithSteps): String {
        val sb = StringBuilder()
        sb.append("# GlyphBar Sequence\n")
        sb.append("# format_version: 1.0\n")
        sb.append("# name: ${playlistWithSteps.playlist.name}\n")
        sb.append("# device: phone_4a\n")
        sb.append("step_index,duration_ms,A1,A2,A3,A4,A5,A6,RED\n")

        val labels = listOf("A1", "A2", "A3", "A4", "A5", "A6", "RED")
        playlistWithSteps.steps.sortedBy { it.stepIndex }.forEachIndexed { index, step ->
            sb.append(index).append(",")
            sb.append(step.durationMs).append(",")
            labels.forEachIndexed { i, label ->
                val intensity = step.channelIntensities[LABEL_TO_CHANNEL[label]] ?: 0
                sb.append(intensity)
                if (i < labels.size - 1) sb.append(",")
            }
            sb.append("\n")
        }

        return sb.toString()
    }
}