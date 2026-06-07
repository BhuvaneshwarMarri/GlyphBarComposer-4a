package com.smaarig.glyphbarcomposer.utils

import com.smaarig.glyphbarcomposer.data.SequenceStep
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

object GlyphSequenceExporter {

    @Serializable
    data class GlyphJsonFormat(
        val filename: String,
        val version: String = "glyphbar composer",
        val sequences: List<List<Int>>
    )

    fun exportToCsv(steps: List<SequenceStep>): String {
        val sb = StringBuilder()
        steps.forEachIndexed { index, step ->
            val x1 = step.channelIntensities.getOrDefault(0, 0)
            val x2 = step.channelIntensities.getOrDefault(1, 0)
            val x3 = step.channelIntensities.getOrDefault(2, 0)
            val x4 = step.channelIntensities.getOrDefault(3, 0)
            val x5 = step.channelIntensities.getOrDefault(4, 0)
            val x6 = step.channelIntensities.getOrDefault(5, 0)
            // x7 is red, map intensity 3 back to 1 if it's > 0
            val x7 = if (step.channelIntensities.getOrDefault(6, 0) > 0) 1 else 0
            val duration = step.durationMs
            
            sb.append("${index},${x1},${x2},${x3},${x4},${x5},${x6},${x7},${duration}\n")
        }
        return sb.toString()
    }

    fun exportToJson(filename: String, steps: List<SequenceStep>): String {
        val sequences = steps.map { step ->
            listOf(
                step.channelIntensities.getOrDefault(0, 0),
                step.channelIntensities.getOrDefault(1, 0),
                step.channelIntensities.getOrDefault(2, 0),
                step.channelIntensities.getOrDefault(3, 0),
                step.channelIntensities.getOrDefault(4, 0),
                step.channelIntensities.getOrDefault(5, 0),
                if (step.channelIntensities.getOrDefault(6, 0) > 0) 1 else 0,
                step.durationMs
            )
        }
        val format = GlyphJsonFormat(filename = filename, sequences = sequences)
        return Json { prettyPrint = true }.encodeToString(format)
    }
}
