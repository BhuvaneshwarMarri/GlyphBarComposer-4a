package com.smaarig.glyphbarcomposer.utils

import com.smaarig.glyphbarcomposer.controller.getIntensityForIndex
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
            val x1 = step.channelIntensities.getIntensityForIndex(0)
            val x2 = step.channelIntensities.getIntensityForIndex(1)
            val x3 = step.channelIntensities.getIntensityForIndex(2)
            val x4 = step.channelIntensities.getIntensityForIndex(3)
            val x5 = step.channelIntensities.getIntensityForIndex(4)
            val x6 = step.channelIntensities.getIntensityForIndex(5)
            // x7 is red, map intensity > 0 back to 1
            val x7 = if (step.channelIntensities.getIntensityForIndex(6) > 0) 1 else 0
            val duration = step.durationMs
            
            sb.append("${index},${x1},${x2},${x3},${x4},${x5},${x6},${x7},${duration}\n")
        }
        return sb.toString()
    }

    fun exportToJson(filename: String, steps: List<SequenceStep>): String {
        val sequences = steps.map { step ->
            listOf(
                step.channelIntensities.getIntensityForIndex(0),
                step.channelIntensities.getIntensityForIndex(1),
                step.channelIntensities.getIntensityForIndex(2),
                step.channelIntensities.getIntensityForIndex(3),
                step.channelIntensities.getIntensityForIndex(4),
                step.channelIntensities.getIntensityForIndex(5),
                if (step.channelIntensities.getIntensityForIndex(6) > 0) 1 else 0,
                step.durationMs
            )
        }
        val format = GlyphJsonFormat(filename = filename, sequences = sequences)
        return Json { prettyPrint = true }.encodeToString(format)
    }
}
