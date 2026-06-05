package com.smaarig.glyphbarcomposer.utils

import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.model.ImportError
import com.smaarig.glyphbarcomposer.model.ImportResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Importer and exporter for GlyphBar sequences in JSON format (.gbseq.json).
 */
object JsonSequenceImporter {

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
     * Parses a JSON input stream into an [ImportResult].
     * Collects all validation errors before returning failure.
     */
    fun parse(inputStream: InputStream): ImportResult {
        val errors = mutableListOf<ImportError>()
        val content = try {
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return ImportResult.Failure(listOf(ImportError("IO_ERROR", "Failed to read file: ${e.message}")))
        }

        val root = try {
            JSONObject(content)
        } catch (e: JSONException) {
            return ImportResult.Failure(listOf(ImportError("JSON_NOT_VALID", "Invalid JSON format: ${e.message}")))
        }

        val seq = root.optJSONObject("glyphbar_sequence")
        if (seq == null) {
            return ImportResult.Failure(listOf(ImportError("JSON_MISSING_ROOT", "Missing 'glyphbar_sequence' root key.")))
        }

        // 1. Basic format validation
        if (seq.optString("type") != "glyphbar_sequence") {
            errors.add(ImportError("JSON_WRONG_TYPE", "Root 'type' must be 'glyphbar_sequence'."))
        }
        if (seq.optString("format_version") != "1.0") {
            errors.add(ImportError("JSON_UNSUPPORTED_VERSION", "Unsupported format version."))
        }
        if (seq.optString("device") != "phone_4a") {
            errors.add(ImportError("JSON_WRONG_DEVICE", "Sequence is not for Phone (4a)."))
        }

        // 2. Metadata validation
        val metadata = seq.optJSONObject("metadata")
        val rawName = metadata?.optString("name") ?: ""
        val name = rawName.trim()
        if (name.isEmpty()) {
            errors.add(ImportError("JSON_NAME_BLANK", "Sequence name is required and cannot be blank."))
        } else if (name.length > 80) {
            errors.add(ImportError("JSON_NAME_TOO_LONG", "Sequence name exceeds 80 characters."))
        }

        // 3. Channels validation
        val channelsArray = seq.optJSONArray("channels")
        val expectedChannels = listOf("A1", "A2", "A3", "A4", "A5", "A6", "RED")
        if (channelsArray == null) {
            errors.add(ImportError("JSON_WRONG_CHANNELS", "Missing 'channels' array."))
        } else {
            val actualChannels = (0 until channelsArray.length()).map { channelsArray.optString(it) }
            if (actualChannels != expectedChannels) {
                errors.add(ImportError("JSON_WRONG_CHANNELS", "Channels must be exactly $expectedChannels."))
            }
        }

        // 4. Steps validation
        val stepsArray = seq.optJSONArray("steps")
        val resultSteps = mutableListOf<GlyphSequence>()
        if (stepsArray == null || stepsArray.length() == 0) {
            errors.add(ImportError("JSON_NO_STEPS", "Sequence must contain at least one step."))
        } else if (stepsArray.length() > 500) {
            errors.add(ImportError("JSON_TOO_MANY_STEPS", "Sequence exceeds maximum limit of 500 steps."))
        } else {
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.optJSONObject(i)
                if (stepObj == null) {
                    errors.add(ImportError("JSON_STEP_INVALID", "Step at index $i is invalid."))
                    continue
                }

                val index = stepObj.optInt("step_index", -1)
                if (index != i) {
                    errors.add(ImportError("JSON_STEP_INDEX_GAP", "Step index mismatch at array position $i."))
                }

                val duration = stepObj.optInt("duration_ms", 0)
                if (duration < 50 || duration > 30000) {
                    errors.add(ImportError("JSON_STEP_DURATION_RANGE", "Step $i duration must be between 50 and 30000 ms."))
                }

                val intensitiesObj = stepObj.optJSONObject("intensities")
                val channelMap = mutableMapOf<Int, Int>()
                if (intensitiesObj == null) {
                    errors.add(ImportError("JSON_STEP_MISSING_INTENSITIES", "Step $i is missing 'intensities' object."))
                } else {
                    expectedChannels.forEach { label ->
                        if (!intensitiesObj.has(label)) {
                            errors.add(ImportError("JSON_STEP_MISSING_CHANNEL", "Step $i is missing intensity for '$label'."))
                        } else {
                            val intensity = intensitiesObj.optInt(label, -1)
                            if (intensity !in 0..3) {
                                errors.add(ImportError("JSON_STEP_BAD_INTENSITY", "Step $i has invalid intensity for '$label' (must be 0-3)."))
                            } else {
                                LABEL_TO_CHANNEL[label]?.let { channelMap[it] = intensity }
                            }
                        }
                    }
                }
                resultSteps.add(GlyphSequence(channelMap, duration))
            }
        }

        return if (errors.isEmpty()) {
            ImportResult.Success(name, resultSteps)
        } else {
            ImportResult.Failure(errors)
        }
    }

    /**
     * Exports a [PlaylistWithSteps] to a JSON string.
     */
    fun export(playlistWithSteps: PlaylistWithSteps): String {
        val root = JSONObject()
        val seq = JSONObject()
        root.put("glyphbar_sequence", seq)

        seq.put("format_version", "1.0")
        seq.put("type", "glyphbar_sequence")

        val metadata = JSONObject()
        metadata.put("name", playlistWithSteps.playlist.name)
        metadata.put("description", "")
        metadata.put("author", "GlyphBar Composer User")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        metadata.put("created_at", sdf.format(Date()))
        metadata.put("app_version", "2.0")
        seq.put("metadata", metadata)

        seq.put("device", "phone_4a")
        seq.put("channels", JSONArray(listOf("A1", "A2", "A3", "A4", "A5", "A6", "RED")))

        val steps = JSONArray()
        playlistWithSteps.steps.sortedBy { it.stepIndex }.forEachIndexed { index, step ->
            val stepObj = JSONObject()
            stepObj.put("step_index", index)
            stepObj.put("duration_ms", step.durationMs)
            
            val intensities = JSONObject()
            LABEL_TO_CHANNEL.forEach { (label, channelId) ->
                intensities.put(label, step.channelIntensities[channelId] ?: 0)
            }
            stepObj.put("intensities", intensities)
            steps.put(stepObj)
        }
        seq.put("steps", steps)

        return root.toString(2)
    }
}
