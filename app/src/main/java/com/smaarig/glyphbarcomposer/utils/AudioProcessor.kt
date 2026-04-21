package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt

object AudioProcessor {
    private const val TAG = "AudioProcessor"

    /**
     * Extracts waveform energy samples from an audio file.
     * Each sample represents the average amplitude in a 50ms window.
     * Values are normalized between 0.0 and 1.0.
     */
    fun extractWaveform(context: Context, uri: Uri, durationMs: Int): List<Float> {
        val waveform = mutableListOf<Float>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) return emptyList()

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isExtractorDone = false
            var isDecoderDone = false

            // We want 1 sample every 50ms
            val sampleIntervalMs = 50
            val numSamplesExpected = durationMs / sampleIntervalMs
            
            // Temporary storage for current window's PCM data
            val windowAmplitudes = mutableListOf<Int>()
            var currentWindowStartTimeUs = 0L

            while (!isDecoderDone) {
                if (!isExtractorDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isExtractorDone = true
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    
                    // Process PCM data (assuming 16-bit PCM)
                    if (outputBuffer != null) {
                        while (outputBuffer.remaining() >= 2) {
                            val sample = outputBuffer.short.toInt()
                            windowAmplitudes.add(abs(sample))
                        }
                    }

                    // Check if we've filled a 50ms window
                    if (info.presentationTimeUs >= currentWindowStartTimeUs + (sampleIntervalMs * 1000)) {
                        val avg = if (windowAmplitudes.isNotEmpty()) windowAmplitudes.average().toFloat() else 0f
                        // Normalize 32768 (max short) to 1.0, with some headroom/compression
                        val normalized = (avg / 15000f).coerceIn(0f, 1f)
                        waveform.add(normalized)
                        
                        windowAmplitudes.clear()
                        currentWindowStartTimeUs = info.presentationTimeUs
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isDecoderDone = true
                    }
                }
            }
            
            // Fill remaining if needed to match duration
            while (waveform.size < numSamplesExpected) waveform.add(0f)

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting waveform: ${e.message}")
        } finally {
            codec?.stop()
            codec?.release()
            extractor.release()
        }

        return waveform
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }
}
